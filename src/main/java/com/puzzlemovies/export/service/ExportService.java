package com.puzzlemovies.export.service;

import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.export.AnkiExportFormatter;
import com.puzzlemovies.export.export.CompleteWordExampleMatches;
import com.puzzlemovies.export.export.DictionaryMatcher;
import com.puzzlemovies.export.export.DictionaryParser;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;
import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.ExportRecordBuilder;
import com.puzzlemovies.export.export.PhraseExample;
import com.puzzlemovies.export.export.VocabularyDeduplicator;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportPhase;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.PuzzleSessionToken;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryClient;
import com.puzzlemovies.export.repo.ExportJobRepository;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.review.ReviewExportSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ExportService {
    private final ExportJobRepository exportJobRepository;
    private final PuzzleSessionTokenRepository tokenRepository;
    private final PuzzleMoviesDictionaryClient dictionaryClient;
    private final DictionaryParser dictionaryParser;
    private final VocabularyDeduplicator deduplicator;
    private final DictionaryMatcher matcher;
    private final ExportRecordBuilder recordBuilder;
    private final AnkiExportFormatter formatter;
    private final ExportProperties properties;
    private final ConcurrentMap<UUID, GeneratedExportData> generatedData = new ConcurrentHashMap<>();

    public ExportService(ExportJobRepository exportJobRepository,
                         PuzzleSessionTokenRepository tokenRepository,
                         PuzzleMoviesDictionaryClient dictionaryClient,
                         DictionaryParser dictionaryParser,
                         VocabularyDeduplicator deduplicator,
                         DictionaryMatcher matcher,
                         ExportRecordBuilder recordBuilder,
                         AnkiExportFormatter formatter,
                         ExportProperties properties) {
        this.exportJobRepository = exportJobRepository;
        this.tokenRepository = tokenRepository;
        this.dictionaryClient = dictionaryClient;
        this.dictionaryParser = dictionaryParser;
        this.deduplicator = deduplicator;
        this.matcher = matcher;
        this.recordBuilder = recordBuilder;
        this.formatter = formatter;
        this.properties = properties;
    }

    @Transactional
    public ExportJob startExport(User user, ExportType type) {
        PuzzleSessionToken token = tokenRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("No session token available"));

        ExportJob job = new ExportJob();
        job.setUser(user);
        job.setType(type);
        job.setStatus(ExportStatus.PENDING);
        job.setPhase(ExportPhase.PENDING);
        job.setProgressPercent(0);
        exportJobRepository.save(job);

        runExportAsync(job.getId(), type, token.getCookieHeader());
        return job;
    }

    @Async("exportTaskExecutor")
    public void runExportAsync(UUID jobId, ExportType type, String cookieHeader) {
        ExportJob job = exportJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Export job not found"));
        Path outputFile = null;
        try {
            start(job);

            List<com.puzzlemovies.export.puzzlemovies.DictionaryPage> wordPages = List.of();
            List<com.puzzlemovies.export.puzzlemovies.DictionaryPage> phrasePages = List.of();

            if (type == ExportType.WORDS || type == ExportType.COMBINED) {
                update(job, ExportPhase.FETCHING_WORDS, 15);
                wordPages = dictionaryClient.fetchWordPages(cookieHeader);
            }

            if (type == ExportType.WORDS || type == ExportType.PHRASES || type == ExportType.COMBINED) {
                update(job, ExportPhase.FETCHING_PHRASES, 35);
                phrasePages = dictionaryClient.fetchPhrasePages(cookieHeader);
            }

            update(job, ExportPhase.PARSING, 55);
            var parsedWords = type == ExportType.PHRASES ? com.puzzlemovies.export.export.DictionaryParseResult.<DictionaryWord>empty() : dictionaryParser.parseWordPages(wordPages);
            var parsedPhrases = dictionaryParser.parsePhrasePages(phrasePages);
            int rejected = parsedWords.unparsedRecordCount() + parsedPhrases.unparsedRecordCount();
            int accepted = parsedWords.records().size() + parsedPhrases.records().size();
            if (rejected > 0 && accepted == 0) throw new IllegalStateException("No source records could be parsed");
            var discrepancies = new java.util.ArrayList<com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy>();
            collectCounterDiscrepancy(wordPages, parsedWords.sourceRecordCount(), discrepancies);
            collectCounterDiscrepancy(phrasePages, parsedPhrases.sourceRecordCount(), discrepancies);
            ExportCompletionInfo completion = ExportCompletionInfo.withWarnings(rejected, discrepancies);
            List<DictionaryWord> words = parsedWords.records();
            List<DictionaryPhrase> phrases = parsedPhrases.records();

            update(job, ExportPhase.DEDUPLICATING, 65);
            words = deduplicator.dedupeWords(words);
            phrases = deduplicator.dedupePhrases(phrases);

            update(job, ExportPhase.MATCHING, 75);
            CompleteWordExampleMatches completeMatches = type == ExportType.PHRASES
                    ? CompleteWordExampleMatches.empty()
                    : matcher.matchAll(words, phrases);
            Map<String, List<PhraseExample>> examplesByWord = matcher.match(words, phrases);

            List<ExportRecord> records = recordBuilder.buildRecords(
                    words,
                    phrases,
                    examplesByWord,
                    type == ExportType.WORDS || type == ExportType.COMBINED,
                    type == ExportType.PHRASES || type == ExportType.COMBINED);

            update(job, ExportPhase.WRITING, 90);
            outputFile = writeOutput(jobId, records);
            var rawEntries = new java.util.ArrayList<com.puzzlemovies.export.export.RawDictionaryEntry>();
            rawEntries.addAll(parsedWords.acceptedSourceEntries());
            rawEntries.addAll(parsedPhrases.acceptedSourceEntries());
            generatedData.put(jobId, new GeneratedExportData(records,
                    new ReviewExportSource(words, phrases, completeMatches), rawEntries, completion));
            job.setErrorMessage(ExportCompletionMetadataCodec.encode(completion));

            job.setOutputFilePath(outputFile.toAbsolutePath().toString());
            job.setOutputFileName(outputFile.getFileName().toString());
            job.setRowCount(records.size());
            job.setStatus(ExportStatus.COMPLETED);
            job.setPhase(ExportPhase.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        } catch (Exception ex) {
            generatedData.remove(jobId);
            if (outputFile != null) {
                try { Files.deleteIfExists(outputFile); } catch (IOException ignored) { /* This job remains unavailable. */ }
            }
            job.setOutputFilePath(null);
            job.setOutputFileName(null);
            job.setRowCount(null);
            job.setStatus(ExportStatus.FAILED);
            job.setPhase(ExportPhase.FAILED);
            job.setErrorMessage(ex instanceof com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryException
                    ? ex.getMessage() : "Export could not be completed. Please try again.");
            job.setCompletedAt(Instant.now());
            job.setProgressPercent(100);
            exportJobRepository.save(job);
        }
    }

    private void start(ExportJob job) {
        job.setStatus(ExportStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setProgressPercent(5);
        exportJobRepository.save(job);
    }

    private void collectCounterDiscrepancy(List<com.puzzlemovies.export.puzzlemovies.DictionaryPage> pages, int retrieved,
            List<com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy> discrepancies) {
        if (!pages.isEmpty()) {
            var first = pages.getFirst();
            int declared = first.inspection().declaredTotal();
            if (declared != retrieved) discrepancies.add(new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                    first.section(), declared, retrieved));
        }
    }

    private void update(ExportJob job, ExportPhase phase, int progressPercent) {
        job.setPhase(phase);
        job.setProgressPercent(progressPercent);
        exportJobRepository.save(job);
    }

    private Path writeOutput(UUID jobId, List<ExportRecord> records) throws IOException {
        Path outputDir = Path.of(properties.getOutputDir());
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("export-" + jobId + ".tsv");
        Files.createFile(outputFile);
        try {
            Files.writeString(outputFile, formatter.formatTsv(records), java.nio.file.StandardOpenOption.WRITE);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            try { Files.deleteIfExists(outputFile); } catch (IOException cleanup) { ex.addSuppressed(cleanup); }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Optional<List<ExportRecord>> generatedRecordsForCompletedExport(User user, UUID exportJobId) {
        return exportJobRepository.findByIdAndUser(exportJobId, user)
                .filter(this::usableCompletion)
                .flatMap(job -> Optional.ofNullable(generatedData.get(job.getId())).map(GeneratedExportData::records));
    }

    @Transactional(readOnly = true)
    public Optional<ReviewExportSource> generatedReviewSourceForCompletedExport(User user, UUID exportJobId) {
        return exportJobRepository.findByIdAndUser(exportJobId, user)
                .filter(this::usableCompletion)
                .flatMap(job -> Optional.ofNullable(generatedData.get(job.getId())).map(GeneratedExportData::reviewSource));
    }
    public ExportCompletionInfo completionInfo(ExportJob job) {
        return ExportCompletionMetadataCodec.decode(job);
    }
    public boolean downloadAvailable(ExportJob job) {
        return usableCompletion(job) && job.getOutputFilePath() != null && Files.isRegularFile(Path.of(job.getOutputFilePath()));
    }
    public boolean reviewCreationAvailable(ExportJob job) {
        return usableCompletion(job) && generatedData.containsKey(job.getId());
    }
    public String safeErrorMessage(ExportJob job) {
        try {
            completionInfo(job);
            return job.getStatus() == ExportStatus.FAILED ? job.getErrorMessage() : null;
        } catch (IllegalStateException ex) { return ex.getMessage(); }
    }
    private boolean usableCompletion(ExportJob job) {
        try { return completionInfo(job) != null; }
        catch (IllegalStateException ignored) { return false; }
    }
}
