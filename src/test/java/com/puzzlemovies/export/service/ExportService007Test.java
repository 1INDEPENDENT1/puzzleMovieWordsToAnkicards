package com.puzzlemovies.export.service;
import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.export.*;
import com.puzzlemovies.export.model.*;
import com.puzzlemovies.export.puzzlemovies.*;
import com.puzzlemovies.export.repo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportService007Test {
    @TempDir Path output;
    record Context(ExportService service, ExportJob job, User user, ExportJobRepository jobs, PuzzleMoviesDictionaryClient client) {}
    Context context(int accepted, int rejected) {
        var jobs = mock(ExportJobRepository.class); var client = mock(PuzzleMoviesDictionaryClient.class);
        var parser = mock(DictionaryParser.class); var user = new User(); user.setId(UUID.randomUUID());
        var job = new ExportJob(); job.setId(UUID.randomUUID()); job.setUser(user); job.setType(ExportType.COMBINED); job.setStatus(ExportStatus.PENDING);
        when(jobs.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobs.findByIdAndUser(job.getId(), user)).thenReturn(Optional.of(job));
        var words = new ArrayList<DictionaryWord>(); var raw = new ArrayList<RawDictionaryEntry>(); var issues = new ArrayList<ParseIssue>();
        for (int i=1;i<=accepted;i++) {
            words.add(new DictionaryWord("Run", Set.of("Р±РµР¶Р°С‚СЊ"), "run", false));
            raw.add(new RawDictionaryEntry(new SourcePosition(DictionarySection.WORD,1,i,null),"Run",List.of("Р±РµР¶Р°С‚СЊ"),List.of(),null,null,null));
        }
        for (int i=1;i<=rejected;i++) issues.add(new ParseIssue(new SourcePosition(DictionarySection.WORD,1,accepted+i,null),ParseIssue.Reason.MISSING_ORIGINAL));
        when(parser.parseWordPages(anyList())).thenReturn(new DictionaryParseResult<>(words,raw,issues,accepted+rejected));
        when(parser.parsePhrasePages(anyList())).thenReturn(DictionaryParseResult.empty());
        var props = new ExportProperties(); props.setOutputDir(output.toString());
        var service = new ExportService(jobs,mock(PuzzleSessionTokenRepository.class),client,parser,new VocabularyDeduplicator(),new DictionaryMatcher(),new ExportRecordBuilder(),new AnkiExportFormatter(),props,
                new ExportSnapshotStore(jobs,mock(VocabularyImportSnapshotRepository.class),new com.fasterxml.jackson.databind.ObjectMapper()),Runnable::run);
        return new Context(service,job,user,jobs,client);
    }
    @Test void partialPreservesAcceptedAndCountsSourceBeforeDedupe() throws Exception {
        var c=context(2,1); c.service.runExportAsync(c.job.getId(),ExportType.COMBINED,"SENTINEL");
        assertEquals(ExportStatus.COMPLETED,c.job.getStatus());
        assertEquals(ExportCompletionInfo.partial(1),ExportCompletionMetadataCodec.decode(c.job));
        assertEquals(1,c.job.getRowCount());
        assertEquals(1,Files.readAllLines(Path.of(c.job.getOutputFilePath())).size());
        assertTrue(Files.isRegularFile(Path.of(c.job.getOutputFilePath())));
    }
    @Test void confirmedEmptyIsSuccessfulButAllRejectedFails() {
        var empty=context(0,0); empty.service.runExportAsync(empty.job.getId(),ExportType.COMBINED,"SENTINEL");
        assertEquals(ExportStatus.COMPLETED,empty.job.getStatus()); assertEquals(0,empty.job.getRowCount());
        assertEquals(ExportCompletionInfo.full(),ExportCompletionMetadataCodec.decode(empty.job));
        var broken=context(0,2); broken.service.runExportAsync(broken.job.getId(),ExportType.COMBINED,"SENTINEL");
        assertEquals(ExportStatus.FAILED,broken.job.getStatus()); assertNull(broken.job.getOutputFilePath());
        assertTrue(!broken.service.reviewCreationAvailable(broken.job));
    }
    @Test void sourceFailureDoesNotExposeRawExceptionOrPublishPartial() throws Exception {
        var c=context(1,0); when(c.client.fetchPhrasePages(anyString())).thenThrow(new IOException("SENTINEL_COOKIE_SHOULD_NOT_APPEAR"));
        c.service.runExportAsync(c.job.getId(),ExportType.COMBINED,"SENTINEL_COOKIE_SHOULD_NOT_APPEAR");
        assertEquals(ExportStatus.FAILED,c.job.getStatus());
        assertFalse(c.job.getErrorMessage().contains("SENTINEL")); assertNull(c.job.getOutputFilePath());
    }
    @Test void failedPublicationRevokesCompletionAndOnlyOwnFile() throws Exception {
        var c=context(1,0); Path other=Files.writeString(output.resolve("existing-user-file.tsv"),"keep");
        doAnswer(inv -> { if(c.job.getStatus()==ExportStatus.COMPLETED) throw new IllegalStateException("save failure"); return c.job; }).when(c.jobs).save(any(ExportJob.class));
        c.service.runExportAsync(c.job.getId(),ExportType.COMBINED,"SENTINEL");
        assertEquals(ExportStatus.FAILED,c.job.getStatus()); assertNull(c.job.getOutputFilePath()); assertNull(c.job.getRowCount());
        assertTrue(!c.service.reviewCreationAvailable(c.job));
        assertEquals("keep",Files.readString(other));
        try(var files=Files.list(output)) {assertEquals(1,files.count());}
    }
    @Test void midWriteFailureRemovesOnlyTheNewArtifact() throws Exception {
        var c=context(1,0);
        Path other=Files.writeString(output.resolve("existing-user-file.tsv"),"keep");
        try (var files=mockStatic(Files.class,CALLS_REAL_METHODS)) {
            files.when(() -> Files.writeString(any(Path.class),any(CharSequence.class),any(OpenOption[].class)))
                    .thenAnswer(inv -> {
                        Path path=inv.getArgument(0);
                        try(var stream=Files.newOutputStream(path,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING)) {
                            stream.write("incomplete".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        throw new IOException("Simulated disk write failure");
                    });
            c.service.runExportAsync(c.job.getId(),ExportType.COMBINED,"FAKE_TEST_ONLY");
        }
        assertEquals(ExportStatus.FAILED,c.job.getStatus());
        assertFalse(Files.exists(output.resolve("export-"+c.job.getId()+".tsv")));
        assertEquals("keep",Files.readString(other));
        assertTrue(!c.service.reviewCreationAvailable(c.job));
    }
    @Test void existingArtifactIsNeverOverwrittenOrDeletedOnCreationFailure() throws Exception {
        var c=context(1,0);
        Path existing=Files.writeString(output.resolve("export-"+c.job.getId()+".tsv"),"existing");
        c.service.runExportAsync(c.job.getId(),ExportType.COMBINED,"FAKE_TEST_ONLY");
        assertEquals(ExportStatus.FAILED,c.job.getStatus());
        assertEquals("existing",Files.readString(existing));
    }
}
