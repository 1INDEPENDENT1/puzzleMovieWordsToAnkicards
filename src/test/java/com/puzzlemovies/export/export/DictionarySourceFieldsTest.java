package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionaryPage;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.support.DictionaryFixtureLoader;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DictionarySourceFieldsTest {
    static final DictionaryParser PARSER = new DictionaryParser(new VocabularyNormalizer(ExportTestFixtures.testLemmatizer()));

    // Explicit inspection isolates extraction from the independently tested page inspector.
    static DictionaryPage page(DictionarySection section, String fixture, int count) {
        return page(section, 1, DictionaryFixtureLoader.html(fixture), count);
    }
    static DictionaryPage page(DictionarySection section, int number, String html, int count) {
        return new DictionaryPage(section, number, URI.create("https://puzzle-movies.com/dictionary?item=" + section.item() + "&page=" + number),
                html, new DictionaryPage.PageInspection(section, count, number, null, count, true, true));
    }

    @Test void matchesAllIndependentWordFieldsBeforeNormalization() {
        var actual = PARSER.parseSourceEntries(page(DictionarySection.WORD, "words-real.html", 36));
        var expected = DictionaryFixtureLoader.expectations("words-expected.json");
        assertEquals(36, actual.size());
        assertEquals(36, expected.size());
        for (int i = 0; i < expected.size(); i++) {
            var a = actual.get(i); var e = expected.get(i);
            assertEquals(e.position(), a.position().rowNumber());
            assertEquals(e.original(), a.sourceText(), "original row " + (i + 1));
            assertEquals(List.of(e.translation()), a.translations(), "translation row " + (i + 1));
            assertTrue(a.examples().isEmpty());
            assertEquals(e.sourceLabel(), a.sourceLabel());
            assertEquals(e.movieTitle(), a.movieTitle());
            assertEquals(e.movieUrl(), a.movieUrl());
        }
        assertEquals("to mince", actual.get(0).sourceText());
        assertEquals("a doofus", actual.get(1).sourceText());
    }

    @Test void ignoresUnrelatedTablesAndCardSelectors() {
        var source = DictionaryFixtureLoader.html("words-real.html") +
                "<table><tr><td>not dictionary</td><td>translation</td></tr></table><div class='entry'><span class='word'>not dictionary</span></div>";
        assertEquals(36, PARSER.parseSourceEntries(page(DictionarySection.WORD, 1, source, 36)).size());
    }
}
