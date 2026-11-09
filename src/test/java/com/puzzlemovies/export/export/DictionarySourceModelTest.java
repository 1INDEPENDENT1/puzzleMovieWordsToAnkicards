package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DictionarySourceModelTest {
    private SourcePosition position(int row) { return new SourcePosition(DictionarySection.WORD, 1, row, null); }
    private RawDictionaryEntry entry(int row) {
        return new RawDictionaryEntry(position(row), "to mince", List.of(), List.of(), null, null, null);
    }
    @Test void countsRejectedPositionsRatherThanIssueCount() {
        var first = entry(1);
        var result = new DictionaryParseResult<>(List.of("accepted"), List.of(first),
                List.of(new ParseIssue(position(2), ParseIssue.Reason.MISSING_ORIGINAL),
                        new ParseIssue(position(2), ParseIssue.Reason.UNUSABLE_IDENTITY)), 2);
        assertEquals(1, result.unparsedRecordCount());
        assertEquals(2, result.sourceRecordCount());
        assertEquals(List.of(first), result.acceptedSourceEntries());
    }
    @Test void preservesIdenticalTextAtDistinctPositionsAndCopiesCollections() {
        var source = new java.util.ArrayList<>(List.of(entry(1), entry(2)));
        var result = new DictionaryParseResult<>(List.of("one", "two"), source, List.of(), 2);
        source.clear();
        assertEquals(2, result.acceptedSourceEntries().size());
        assertNotEquals(result.acceptedSourceEntries().get(0).position(), result.acceptedSourceEntries().get(1).position());
        assertThrows(UnsupportedOperationException.class, () -> result.records().clear());
        assertTrue(result.acceptedSourceEntries().get(0).translations().isEmpty());
        assertTrue(result.acceptedSourceEntries().get(0).examples().isEmpty());
        assertNull(result.acceptedSourceEntries().get(0).movieTitle());
    }
    @Test void rejectsUnaccountedRowsAndAcceptedRejectedOverlap() {
        assertThrows(IllegalArgumentException.class,
                () -> new DictionaryParseResult<>(List.of("one"), List.of(entry(1)), List.of(), 2));
        assertThrows(IllegalArgumentException.class,
                () -> new DictionaryParseResult<>(List.of("one"), List.of(entry(1)),
                        List.of(new ParseIssue(position(1), ParseIssue.Reason.MISSING_ORIGINAL)), 2));
    }
    @Test void supportsConfirmedEmptyWithoutRejections() {
        var result = DictionaryParseResult.empty();
        assertEquals(0, result.sourceRecordCount());
        assertEquals(0, result.unparsedRecordCount());
        assertTrue(result.records().isEmpty());
    }
}
