package com.puzzlemovies.export.service;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExportCompletionMetadataCodecTest {
    private ExportJob completed(String metadata) {
        var job = new ExportJob(); job.setStatus(ExportStatus.COMPLETED); job.setErrorMessage(metadata); return job;
    }
    @Test void roundTripsPartialAndLeavesLegacyFullUntouched() {
        var partial = ExportCompletionInfo.partial(2);
        String stored = ExportCompletionMetadataCodec.encode(partial);
        assertEquals(partial, ExportCompletionMetadataCodec.decode(completed(stored)));
        assertTrue(stored.startsWith("DICT_META_V1:"));
        assertTrue(stored.length() < 4000);
        assertEquals(ExportCompletionInfo.full(), ExportCompletionMetadataCodec.decode(completed(null)));
        assertNull(ExportCompletionMetadataCodec.encode(ExportCompletionInfo.full()));
    }
    @Test void rejectsEntireUnknownOrMalformedMarkerFamily() {
        for (String marker : new String[]{"DICT_META_V2:{}", "DICT_META_V1:no-json", "DICT_META_V1:{}",
                "DICT_META_V1:{\"completeness\":\"PARTIAL\",\"unparsedRecordCount\":0}",
                "DICT_META_V1:{\"completeness\":\"PARTIAL\",\"unparsedRecordCount\":1.5}",
                "DICT_META_V1:{\"completeness\":\"PARTIAL\",\"unparsedRecordCount\":2147483648}",
                ExportCompletionMetadataCodec.encode(ExportCompletionInfo.partial(1)) + " ".repeat(4000)})
            assertThrows(IllegalStateException.class, () -> ExportCompletionMetadataCodec.decode(completed(marker)));
    }
    @Test void runningAndFailedJobsHaveNoCompletionProjection() {
        var job = new ExportJob(); job.setStatus(ExportStatus.RUNNING);
        assertNull(ExportCompletionMetadataCodec.decode(job));
        job.setStatus(ExportStatus.FAILED); job.setErrorMessage("Safe failure");
        assertNull(ExportCompletionMetadataCodec.decode(job));
        assertThrows(IllegalArgumentException.class, () -> ExportCompletionInfo.partial(0));
    }
    @Test void rejectsTrailingJsonInsteadOfSilentlyAcceptingCorruption() {
        String valid = ExportCompletionMetadataCodec.encode(ExportCompletionInfo.partial(1));
        assertThrows(IllegalStateException.class, () -> ExportCompletionMetadataCodec.decode(completed(valid + " {}")));
    }
    @Test void counterDiscrepancyIsDurableAndSeparateFromRejectedRows() {
        var mismatch = new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, 766, 756);
        var completion = ExportCompletionInfo.withWarnings(0, java.util.List.of(mismatch));
        assertEquals(ExportCompletionInfo.Completeness.PARTIAL, completion.completeness());
        assertEquals(0, completion.unparsedRecordCount());
        assertTrue(completion.warningMessage().contains("766"));
        assertTrue(completion.warningMessage().contains("756"));
        assertFalse(completion.warningMessage().contains("could not be parsed"));
        assertEquals(completion, ExportCompletionMetadataCodec.decode(completed(ExportCompletionMetadataCodec.encode(completion))));
    }
    @Test void rejectsMalformedCounterWarningsAndKeepsBothValidWarningCauses() {
        var d = new com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy(
                com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, 766, 756);
        var combined = ExportCompletionInfo.withWarnings(2, java.util.List.of(d));
        assertEquals(2, combined.unparsedRecordCount());
        assertTrue(combined.warningMessage().contains("2 source record(s) could not be parsed"));
        assertTrue(combined.warningMessage().contains("site reports 766"));
        assertEquals(combined, ExportCompletionMetadataCodec.decode(completed(ExportCompletionMetadataCodec.encode(combined))));
        String valid = ExportCompletionMetadataCodec.encode(ExportCompletionInfo.withWarnings(0, java.util.List.of(d)));
        for (String invalid : java.util.List.of(valid.replace("766", "-1"), valid.replace("766", "756"),
                valid.replace("PHRASE", "UNKNOWN"), valid.replace("756", "1.2")))
            assertThrows(IllegalStateException.class, () -> ExportCompletionMetadataCodec.decode(completed(invalid)));
        assertThrows(IllegalArgumentException.class, () -> ExportCompletionInfo.withWarnings(0, java.util.List.of(d,d)));
    }
}
