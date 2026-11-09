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
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryClient;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesSessionExpiredException;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.review.ReviewCardDraftFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

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
    void stableSourceCounterMismatchWarnsSeparatelyFromParseRejections() throws Exception {
        var c = context(ExportType.COMBINED);
        var page = ExportTestFixtures.wordPages().getFirst();
        var mismatched = new com.puzzlemovies.export.puzzlemovies.DictionaryPage(page.section(), 1, page.sourceUri(), page.html(),
                new com.puzzlemovies.export.puzzlemovies.DictionaryPage.PageInspection(page.section(), 13, 1, null, 3, true, true));
        when(c.dictionaryClient.fetchWordPages(anyString())).thenReturn(java.util.List.of(mismatched));
        when(c.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(ExportTestFixtures.phrasePages());
        c.service.runExportAsync(c.job.getId(), ExportType.COMBINED, "FAKE_TEST_ONLY");
        assertEquals(ExportStatus.COMPLETED, c.job.getStatus());
        var completion = ExportCompletionMetadataCodec.decode(c.job);
        assertEquals(ExportCompletionInfo.Completeness.PARTIAL, completion.completeness());
        assertEquals(0, completion.unparsedRecordCount());
        assertEquals(java.util.List.of(new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(page.section(), 13, 3)),
                completion.sourceCountDiscrepancies());
        assertEquals(5, c.job.getRowCount());
        assertTrue(c.service.generatedReviewSourceForCompletedExport(c.user, c.job.getId()).isPresent());
    }

    @Test
    void realSourceFixturesRespectAllModesAndKeepTwoColumnOutput() throws Exception {
        var words = java.util.List.of(ExportTestFixtures.page(
                com.puzzlemovies.export.puzzlemovies.DictionarySection.WORD,
                com.puzzlemovies.export.support.DictionaryFixtureLoader.html("words-real.html"), 36));
        var phrases = java.util.List.of(ExportTestFixtures.page(
                com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE,
                com.puzzlemovies.export.support.DictionaryFixtureLoader.html("phrases-real.html"), 36));
        var phraseExpectations = com.puzzlemovies.export.support.DictionaryFixtureLoader.expectations("phrases-expected.json");
        for (ExportType type : ExportType.values()) {
            var c = context(type);
            if (type != ExportType.PHRASES) when(c.dictionaryClient.fetchWordPages(anyString())).thenReturn(words);
            when(c.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(phrases);
            c.service.runExportAsync(c.job.getId(), type, "FAKE_TEST_ONLY");
            assertEquals(ExportStatus.COMPLETED, c.job.getStatus(), type.name());
            assertEquals(ExportCompletionInfo.full(), ExportCompletionMetadataCodec.decode(c.job));
            var records = c.service.generatedRecordsForCompletedExport(c.user, c.job.getId()).orElseThrow();
            var source = c.service.generatedReviewSourceForCompletedExport(c.user, c.job.getId()).orElseThrow();
            assertEquals(36, source.examples().size());
            assertEquals(phraseExpectations.get(0).original(), source.examples().get(0).sourceText());
            String output = Files.readString(Path.of(c.job.getOutputFilePath()));
            assertEquals(records.size(), output.lines().count());
            assertTrue(output.lines().allMatch(line -> line.split("\t", -1).length == 2));
            if (type == ExportType.WORDS) {
                assertTrue(records.stream().allMatch(r -> r.recordKind() == com.puzzlemovies.export.export.ExportRecord.RecordKind.WORD));
                assertEquals("to mince", records.get(0).front());
                assertTrue(records.get(0).back().contains("смягчать"));
            } else {
                assertEquals(36, records.stream().filter(r -> r.recordKind() == com.puzzlemovies.export.export.ExportRecord.RecordKind.PHRASE).count());
                assertTrue(records.stream().anyMatch(r -> r.front().equals("Man, heck of a win!")));
            }
            if (type == ExportType.PHRASES) {
                verify(c.dictionaryClient, never()).fetchWordPages(anyString());
                assertTrue(source.words().isEmpty());
            } else {
                assertEquals("to mince", source.words().get(0).sourceText());
                verify(c.dictionaryClient).fetchWordPages(anyString());
            }
            verify(c.dictionaryClient).fetchPhrasePages(anyString());
        }
    }

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

        assertTrue(context.service.generatedReviewSourceForCompletedExport(context.user, context.job.getId()).isPresent());
        assertEquals(2, context.service.generatedReviewSourceForCompletedExport(context.user, context.job.getId())
                .orElseThrow().examples().size());
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
        assertTrue(context.job.getErrorMessage().contains("AUTHENTICATION"));
        assertTrue(context.job.getErrorMessage().contains("section=WORD, page=1"));
        assertNotNull(context.job.getCompletedAt());
        verify(context.exportJobRepository, atLeastOnce()).save(context.job);
    }

    @Test
    void mixedFiftyItemExportRetainsEveryStructuredReviewContextWithoutChangingTsvRows() throws Exception {
        TestContext context = context(ExportType.COMBINED);
        String wordRows = IntStream.range(0, 50)
                .mapToObj(index -> ExportTestFixtures.wordRow("word" + index, "translation" + index))
                .collect(java.util.stream.Collectors.joining());
        String phraseRows = IntStream.range(0, 50)
                .mapToObj(index -> ExportTestFixtures.phraseRow("word" + index + " example", "example translation" + index, ""))
                .collect(java.util.stream.Collectors.joining());
        when(context.dictionaryClient.fetchWordPages(anyString())).thenReturn(java.util.List.of(ExportTestFixtures.page(com.puzzlemovies.export.puzzlemovies.DictionarySection.WORD, "<table class='dict__video__list-table'>" + wordRows + "</table>", 50)));
        when(context.dictionaryClient.fetchPhrasePages(anyString())).thenReturn(java.util.List.of(ExportTestFixtures.page(com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, "<table class='dict__video__list-table'>" + phraseRows + "</table>", 50)));

        context.service.runExportAsync(context.job.getId(), ExportType.COMBINED, "cookie=value");

        var source = context.service.generatedReviewSourceForCompletedExport(context.user, context.job.getId()).orElseThrow();
        var drafts = new ReviewCardDraftFactory().createDrafts(source);
        assertEquals(100, context.job.getRowCount());
        assertEquals(50, drafts.size());
        assertEquals(50, new java.util.HashSet<>(drafts).size());
        assertEquals(50, source.completeMatches().examplesByWord().size());
    }

    private TestContext context(ExportType type) {
        ExportJobRepository exportJobRepository = mock(ExportJobRepository.class);
        PuzzleSessionTokenRepository tokenRepository = mock(PuzzleSessionTokenRepository.class);
        PuzzleMoviesDictionaryClient dictionaryClient = mock(PuzzleMoviesDictionaryClient.class);
        ExportJob job = new ExportJob();
        job.setId(UUID.randomUUID());
        job.setType(type);
        job.setStatus(ExportStatus.PENDING);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("learner@example.com");
        job.setUser(user);

        when(exportJobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(exportJobRepository.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));

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

        return new TestContext(service, exportJobRepository, dictionaryClient, job, user);
    }

    private record TestContext(ExportService service,
                               ExportJobRepository exportJobRepository,
                               PuzzleMoviesDictionaryClient dictionaryClient,
                               ExportJob job,
                               User user) {
    }
}
