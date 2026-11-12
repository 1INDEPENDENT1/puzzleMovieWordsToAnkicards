package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.repo.VocabularyImportSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReviewCardImportService {
    private final LinkedVocabularyService vocabulary;
    private final VocabularyImportSnapshotRepository snapshots;
    private final ExportSnapshotStore store;

    public ReviewCardImportService(LinkedVocabularyService vocabulary, VocabularyImportSnapshotRepository snapshots,
                                   ExportSnapshotStore store) {
        this.vocabulary = vocabulary;
        this.snapshots = snapshots;
        this.store = store;
    }

    @Transactional
    public ImportResult importSnapshot(User user, UUID jobId) {
        User owner = vocabulary.lockOwner(user.getId());
        var snapshot = snapshots.findByUser_IdAndExportJob_Id(owner.getId(), jobId)
                .orElseThrow(SnapshotUnavailable::new);
        if (snapshot.getSchemaVersion() != VocabularyImportSnapshot.SCHEMA_VERSION) throw new SnapshotUnavailable();
        store.verify(snapshot);
        var entries = snapshot.getPayload().entries();
        long examples = entries.stream().mapToLong(row ->
                row.position().section() == DictionarySection.PHRASE
                        ? 1 : row.examples().size()).sum();
        if (snapshot.getPublishedAt() != null) {
            return new ImportResult(0, examples, true);
        }
        Instant now = Instant.now();
        int created = vocabulary.publish(owner, snapshot.getId(), entries, now);
        snapshot.setPublishedAt(now);
        return new ImportResult(created, examples, false);
    }

    public record ImportResult(int created, long examples, boolean alreadyPublished) {}
    public static class SnapshotUnavailable extends RuntimeException {}
}
