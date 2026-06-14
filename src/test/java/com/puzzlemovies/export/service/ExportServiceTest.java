package com.puzzlemovies.export.service;

import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.export.AnkiExportFormatter;
import com.puzzlemovies.export.export.DictionaryMatcher;
import com.puzzlemovies.export.export.DictionaryParser;
import com.puzzlemovies.export.export.ExportRecordBuilder;
import com.puzzlemovies.export.export.ExportTestFixtures;
import com.puzzlemovies.export.export.VocabularyDeduplicator;
import com.puzzlemovies.export.export.VocabularyNormalizer;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportPhase;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryClient;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesSessionExpiredException;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void combinedExportWritesWordAndPhraseRowsWithPhaseAndRowCount() throws Exception {
        TestContext context = context(ExportType.COMBINED);
        when(context.dictionaryClient.fetchWordPages(anyString())).thenReturn(ExportTestFixtures.wordPages());
        when(context.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(ExportTestFixtures.phrasePages());

        context.service.runExportAsync(context.job.getId(), ExportType.COMBINED, "cookie=value");

        assertEquals(ExportStatus.COMPLETED, context.job.getStatus());
        assertEquals(ExportPhase.COMPLETED, context.job.getPhase());
        assertEquals(5, context.job.getRowCount());
        String output = Files.readString(Path.of(context.job.getOutputFilePath()));
        assertEquals(5, output.lines().count());
        assertTrue(output.contains("Run\t"));
        assertTrue(output.contains("I am running home."));
        assertTrue(output.contains("The moon is bright."));
    }

    @Test
    void wordsExportFetchesPhrasesForExamplesWithoutStandalonePhraseRows() throws Exception {
        TestContext context = context(ExportType.WORDS);
        when(context.dictionaryClient.fetchWordPages(anyString())).thenReturn(ExportTestFixtures.wordPages());
        when(context.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(ExportTestFixtures.phrasePages());

        context.service.runExportAsync(context.job.getId(), ExportType.WORDS, "cookie=value");

        assertEquals(3, context.job.getRowCount());
        String output = Files.readString(Path.of(context.job.getOutputFilePath()));
        assertTrue(output.contains("I am running home."));
        assertTrue(output.contains("The moon is bright."));
        assertTrue(output.lines().noneMatch(line -> line.startsWith("I am running home.\t")));
    }

    @Test
    void phraseOnlyAndEmptyExportsComplete() throws Exception {
        TestContext phraseContext = context(ExportType.PHRASES);
        when(phraseContext.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(ExportTestFixtures.phrasePages());

        phraseContext.service.runExportAsync(phraseContext.job.getId(), ExportType.PHRASES, "cookie=value");

        assertEquals(2, phraseContext.job.getRowCount());
        verify(phraseContext.dictionaryClient, never()).fetchWordPages(anyString());

        TestContext emptyContext = context(ExportType.COMBINED);
        when(emptyContext.dictionaryClient.fetchWordPages(anyString())).thenReturn(java.util.List.of());
        when(emptyContext.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(java.util.List.of());

        emptyContext.service.runExportAsync(emptyContext.job.getId(), ExportType.COMBINED, "cookie=value");

        assertEquals(0, emptyContext.job.getRowCount());
        assertEquals("", Files.readString(Path.of(emptyContext.job.getOutputFilePath())));
    }

    @Test
    void marksJobFailedWhenPuzzleMoviesReturnsAnonymousPage() throws Exception {
        TestContext context = context(ExportType.WORDS);
        when(context.dictionaryClient.fetchWordPages(anyString())).thenThrow(new PuzzleMoviesSessionExpiredException());

        context.service.runExportAsync(context.job.getId(), ExportType.WORDS, "wp_logged_in_cookie=stale");

        assertEquals(ExportStatus.FAILED, context.job.getStatus());
        assertEquals(ExportPhase.FAILED, context.job.getPhase());
        assertEquals(100, context.job.getProgressPercent());
        assertEquals("Puzzle-Movies session expired or returned an anonymous page. Please sign in again.",
                context.job.getErrorMessage());
        assertNotNull(context.job.getCompletedAt());
        verify(context.exportJobRepository, atLeastOnce()).save(context.job);
    }

    private TestContext context(ExportType type) {
        ExportJobRepository exportJobRepository = mock(ExportJobRepository.class);
        PuzzleSessionTokenRepository tokenRepository = mock(PuzzleSessionTokenRepository.class);
        PuzzleMoviesDictionaryClient dictionaryClient = mock(PuzzleMoviesDictionaryClient.class);
        ExportJob job = new ExportJob();
        job.setId(UUID.randomUUID());
        job.setType(type);
        job.setStatus(ExportStatus.PENDING);

        when(exportJobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        ExportProperties properties = new ExportProperties();
        properties.setOutputDir(tempDir.toString());

        ExportService service = new ExportService(
                exportJobRepository,
                tokenRepository,
                dictionaryClient,
                new DictionaryParser(new VocabularyNormalizer(ExportTestFixtures.testLemmatizer())),
                new VocabularyDeduplicator(),
                new DictionaryMatcher(),
                new ExportRecordBuilder(),
                new AnkiExportFormatter(),
                properties);

        return new TestContext(service, exportJobRepository, dictionaryClient, job);
    }

    private record TestContext(ExportService service,
                               ExportJobRepository exportJobRepository,
                               PuzzleMoviesDictionaryClient dictionaryClient,
                               ExportJob job) {
    }
}
