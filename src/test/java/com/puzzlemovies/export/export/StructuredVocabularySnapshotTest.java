package com.puzzlemovies.export.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.service.ExportSnapshotStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StructuredVocabularySnapshotTest {
    @Test
    void keepsIndependentSourceFieldsAcrossSerialization() throws Exception {
        var inline = new SourceExample("Give it up.", List.of("Сдавайся."), "inline", "Film C", null);
        var word = new RawDictionaryEntry(new SourcePosition(DictionarySection.WORD, 1, 3, "w1"),
                "give up", List.of("сдаваться"), List.of(inline), null, null, null);
        var first = new RawDictionaryEntry(new SourcePosition(DictionarySection.PHRASE, 1, 2, "p1"),
                "Give it up.", List.of("Сдавайся."), List.of(), "saved", "Film A", "https://example.com/a");
        var second = new RawDictionaryEntry(new SourcePosition(DictionarySection.PHRASE, 2, 1, "p2"),
                first.sourceText(), List.of("Брось это."), List.of(), "saved", "Film B", "https://example.com/b");
        var payload = new VocabularyImportSnapshot.Payload(List.of(word, first, second), List.of("Partial source"));
        var mapper = new ObjectMapper();
        var restored = mapper.readValue(mapper.writeValueAsBytes(payload), VocabularyImportSnapshot.Payload.class);
        assertEquals(payload, restored);
        assertEquals(ExportSnapshotStore.checksum(mapper, payload), ExportSnapshotStore.checksum(mapper, restored));
    }
}
