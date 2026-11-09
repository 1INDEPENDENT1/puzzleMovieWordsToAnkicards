package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.support.DictionaryFixtureLoader;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.puzzlemovies.export.export.DictionarySourceFieldsTest.*;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryPhraseSourceTest {
    @Test void matchesAllIndependentPhraseFieldsAndPunctuation() {
        var actual = PARSER.parseSourceEntries(page(DictionarySection.PHRASE, "phrases-real.html", 36));
        var expected = DictionaryFixtureLoader.expectations("phrases-expected.json");
        assertEquals(36, actual.size()); assertEquals(36, expected.size());
        for (int i = 0; i < expected.size(); i++) {
            var a = actual.get(i); var e = expected.get(i);
            assertEquals(e.position(), a.position().rowNumber());
            assertEquals(e.original(), a.sourceText(), "original row " + (i + 1));
            assertEquals(List.of(e.translation()), a.translations());
            assertTrue(a.examples().isEmpty());
            assertEquals(e.sourceLabel(), a.sourceLabel());
            assertEquals(e.movieTitle(), a.movieTitle());
            assertEquals(e.movieUrl(), a.movieUrl());
        }
        assertEquals("Man, heck of a win!", actual.get(0).sourceText());
        assertEquals("You don't mince words around here, do you?", actual.get(1).sourceText());
    }
}
