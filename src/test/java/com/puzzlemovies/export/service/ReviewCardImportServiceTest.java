package com.puzzlemovies.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.export.SourcePosition;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.VocabularyImportSnapshotRepository;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewCardImportServiceTest {
    @Test
    void publishesOwnerSnapshotOnceAndCountsPhraseOnlyInputWithoutCards() {
        var owner = ReviewTestFixtures.user("import@example.com");
        var job = new ExportJob();
        job.setId(UUID.randomUUID());
        var row = new RawDictionaryEntry(new SourcePosition(DictionarySection.PHRASE, 1, 1, "p1"),
                "Give it up.", List.of("Сдавайся."), List.of(), null, "Film", null);
        var payload = new VocabularyImportSnapshot.Payload(List.of(row), List.of());
        var mapper = new ObjectMapper();
        var snapshot = new VocabularyImportSnapshot(owner, job, payload, ExportSnapshotStore.checksum(mapper, payload));
        var repository = mock(VocabularyImportSnapshotRepository.class);
        var vocabulary = mock(LinkedVocabularyService.class);
        when(vocabulary.lockOwner(owner.getId())).thenReturn(owner);
        when(repository.findByUser_IdAndExportJob_Id(owner.getId(), job.getId())).thenReturn(Optional.of(snapshot));
        var service = new ReviewCardImportService(vocabulary, repository,
                new ExportSnapshotStore(mock(ExportJobRepository.class), repository, mapper));

        assertEquals(new ReviewCardImportService.ImportResult(0, 1, false), service.importSnapshot(owner, job.getId()));
        assertNotNull(snapshot.getPublishedAt());
        assertEquals(new ReviewCardImportService.ImportResult(0, 1, true), service.importSnapshot(owner, job.getId()));
        verify(vocabulary).publish(eq(owner), eq(snapshot.getId()), eq(List.of(row)), any());
    }

    @Test
    void rejectsMissingOrCorruptedSnapshotBeforePublishingAnything() {
        var owner = ReviewTestFixtures.user("import@example.com");
        var job = new ExportJob();
        job.setId(UUID.randomUUID());
        var repository = mock(VocabularyImportSnapshotRepository.class);
        var vocabulary = mock(LinkedVocabularyService.class);
        when(vocabulary.lockOwner(owner.getId())).thenReturn(owner);
        var store = new ExportSnapshotStore(mock(ExportJobRepository.class), repository, new ObjectMapper());
        var service = new ReviewCardImportService(vocabulary, repository, store);
        assertThrows(ReviewCardImportService.SnapshotUnavailable.class, () -> service.importSnapshot(owner, job.getId()));
        var snapshot = new VocabularyImportSnapshot(owner, job,
                new VocabularyImportSnapshot.Payload(List.of(), List.of()), "a".repeat(64));
        when(repository.findByUser_IdAndExportJob_Id(owner.getId(), job.getId())).thenReturn(Optional.of(snapshot));
        assertThrows(IllegalStateException.class, () -> service.importSnapshot(owner, job.getId()));
        assertNull(snapshot.getPublishedAt());
        verify(vocabulary, never()).publish(any(), any(), any(), any());
    }
}
