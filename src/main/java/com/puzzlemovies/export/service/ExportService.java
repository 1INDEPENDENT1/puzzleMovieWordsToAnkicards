package com.puzzlemovies.export.service;

import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.export.AnkiExportFormatter;
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
    private final ConcurrentMap<UUID, List<ExportRecord>> generatedRecordsByJobId = new ConcurrentHashMap<>();

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
        try {
            start(job);

            List<String> wordPages = List.of();
            List<String> phrasePages = List.of();

            if (type == ExportType.WORDS || type == ExportType.COMBINED) {
                update(job, ExportPhase.FETCHING_WORDS, 15);
                wordPages = dictionaryClient.fetchWordPages(cookieHeader);
            }

            if (type == ExportType.WORDS || type == ExportType.PHRASES || type == ExportType.COMBINED) {
                update(job, ExportPhase.FETCHING_PHRASES, 35);
                phrasePages = dictionaryClient.fetchPhrasePages(cookieHeader);
            }

            update(job, ExportPhase.PARSING, 55);
            List<DictionaryWord> words = type == ExportType.PHRASES ? List.of() : dictionaryParser.parseWords(wordPages);
            List<DictionaryPhrase> phrases = dictionaryParser.parsePhrases(phrasePages);

            update(job, ExportPhase.DEDUPLICATING, 65);
            words = deduplicator.dedupeWords(words);
            phrases = deduplicator.dedupePhrases(phrases);

            update(job, ExportPhase.MATCHING, 75);
            Map<String, List<PhraseExample>> examplesByWord = type == ExportType.PHRASES
                    ? Map.of()
                    : matcher.match(words, phrases);

            List<ExportRecord> records = recordBuilder.buildRecords(
                    words,
                    phrases,
                    examplesByWord,
                    type == ExportType.WORDS || type == ExportType.COMBINED,
                    type == ExportType.PHRASES || type == ExportType.COMBINED);

            update(job, ExportPhase.WRITING, 90);
            Path outputFile = writeOutput(jobId, records);
            generatedRecordsByJobId.put(jobId, List.copyOf(records));

            job.setOutputFilePath(outputFile.toAbsolutePath().toString());
            job.setOutputFileName(outputFile.getFileName().toString());
            job.setRowCount(records.size());
            job.setStatus(ExportStatus.COMPLETED);
            job.setPhase(ExportPhase.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(ExportStatus.FAILED);
            job.setPhase(ExportPhase.FAILED);
            job.setErrorMessage(ex.getMessage());
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

    private void update(ExportJob job, ExportPhase phase, int progressPercent) {
        job.setPhase(phase);
        job.setProgressPercent(progressPercent);
        exportJobRepository.save(job);
    }

    private Path writeOutput(UUID jobId, List<ExportRecord> records) throws IOException {
        Path outputDir = Path.of(properties.getOutputDir());
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("export-" + jobId + ".tsv");
        Files.writeString(outputFile, formatter.formatTsv(records));
        return outputFile;
    }

    @Transactional(readOnly = true)
    public Optional<List<ExportRecord>> generatedRecordsForCompletedExport(User user, UUID exportJobId) {
        return exportJobRepository.findByIdAndUser(exportJobId, user)
                .filter(job -> job.getStatus() == ExportStatus.COMPLETED)
                .flatMap(job -> Optional.ofNullable(generatedRecordsByJobId.get(job.getId())));
    }
}
