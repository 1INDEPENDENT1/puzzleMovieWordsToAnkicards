package com.puzzlemovies.export.service;

import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.export.DictionaryEntry;
import com.puzzlemovies.export.export.DictionaryParser;
import com.puzzlemovies.export.model.ExportJob;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExportService {
    private final ExportJobRepository exportJobRepository;
    private final PuzzleSessionTokenRepository tokenRepository;
    private final PuzzleMoviesDictionaryClient dictionaryClient;
    private final DictionaryParser dictionaryParser;
    private final ExportProperties properties;

    public ExportService(ExportJobRepository exportJobRepository,
                         PuzzleSessionTokenRepository tokenRepository,
                         PuzzleMoviesDictionaryClient dictionaryClient,
                         DictionaryParser dictionaryParser,
                         ExportProperties properties) {
        this.exportJobRepository = exportJobRepository;
        this.tokenRepository = tokenRepository;
        this.dictionaryClient = dictionaryClient;
        this.dictionaryParser = dictionaryParser;
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
            job.setStatus(ExportStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setProgressPercent(5);
            exportJobRepository.save(job);

            List<String> wordPages = List.of();
            List<String> phrasePages = List.of();

            if (type == ExportType.WORDS || type == ExportType.COMBINED) {
                wordPages = dictionaryClient.fetchWordPages(cookieHeader);
            }
            job.setProgressPercent(35);
            exportJobRepository.save(job);

            if (type == ExportType.PHRASES || type == ExportType.COMBINED) {
                phrasePages = dictionaryClient.fetchPhrasePages(cookieHeader);
            }
            job.setProgressPercent(55);
            exportJobRepository.save(job);

            List<DictionaryEntry> entries = new ArrayList<>();
            if (!wordPages.isEmpty()) {
                entries.addAll(dictionaryParser.parseWords(wordPages));
            }
            if (!phrasePages.isEmpty()) {
                entries.addAll(dictionaryParser.parsePhrases(phrasePages));
            }

            job.setProgressPercent(75);
            exportJobRepository.save(job);

            Path outputDir = Path.of(properties.getOutputDir());
            Files.createDirectories(outputDir);
            String fileName = "export-" + jobId + ".tsv";
            Path outputFile = outputDir.resolve(fileName);
            writeTsv(outputFile, entries);

            job.setOutputFilePath(outputFile.toAbsolutePath().toString());
            job.setOutputFileName(fileName);
            job.setRowCount(entries.size());
            job.setStatus(ExportStatus.COMPLETED);
            job.setProgressPercent(100);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(Instant.now());
            job.setProgressPercent(100);
            exportJobRepository.save(job);
        }
    }

    private void writeTsv(Path outputFile, List<DictionaryEntry> entries) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            for (DictionaryEntry entry : entries) {
                writer.write(entry.getSource());
                writer.write('\t');
                writer.write(entry.getTranslation());
                writer.newLine();
            }
        }
    }
}
