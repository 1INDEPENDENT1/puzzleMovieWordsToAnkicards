package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.support.DictionaryFixtureLoader;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.puzzlemovies.export.export.DictionarySourceFieldsTest.*;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryPhraseSourceTest {
    @Test void pairsFlatInlineTranslationsWithTheirPrecedingExamples() {
        var doc = org.jsoup.Jsoup.parse(DictionaryFixtureLoader.html("edge-cases/words-inline-example-derived.html"));
        doc.selectFirst("td.dict__video__list-table__phrase").html("""
                <div class="dict__video__list-table__phrase__eng">You mince words.</div>
                <div class="dict__video__list-table__phrase__rus">Вы говорите мягко.</div>
                <div class="dict__video__list-table__phrase__rus">Вы смягчаете слова.</div>
                <div class="dict__video__list-table__phrase__eng">We do not mince words.</div>
                <div class="dict__video__list-table__phrase__rus">Мы говорим прямо.</div>
                """);

        var result = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, 1, doc.outerHtml(), 1)));
        var inline = result.acceptedSourceEntries().getFirst().examples();

        assertEquals(List.of("You mince words.", "We do not mince words."),
                inline.stream().map(SourceExample::sourceText).toList());
        assertEquals(List.of(List.of("Вы говорите мягко.", "Вы смягчаете слова."), List.of("Мы говорим прямо.")),
                inline.stream().map(SourceExample::translations).toList());
    }

    @Test void preservesEveryInlineExampleAndItsOwnTranslation() {
        var doc = org.jsoup.Jsoup.parse(DictionaryFixtureLoader.html("edge-cases/words-inline-example-derived.html"));
        doc.selectFirst("td.dict__video__list-table__phrase").html("""
                <div data-id="first">
                  <a class="dict__video__list-table__info__balloon__title" href="/movie/first">First film</a>
                  <div class="dict__video__list-table__phrase__eng">You mince words.</div>
                  <div class="dict__video__list-table__phrase__rus">Вы говорите мягко.</div>
                </div>
                <div data-id="second">
                  <div class="dict__video__list-table__phrase__eng">We do not mince words.</div>
                  <div class="dict__video__list-table__phrase__rus">Мы говорим прямо.</div>
                </div>
                """);

        var result = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, 1, doc.outerHtml(), 1)));
        var raw = result.acceptedSourceEntries().getFirst();

        assertEquals(List.of("You mince words.", "We do not mince words."),
                raw.examples().stream().map(SourceExample::sourceText).toList());
        assertEquals(List.of(List.of("Вы говорите мягко."), List.of("Мы говорим прямо.")),
                raw.examples().stream().map(SourceExample::translations).toList());
        assertEquals(List.of("first", "second"), raw.examples().stream().map(SourceExample::stableSourceId).toList());
        assertEquals("First film", raw.examples().getFirst().movieTitle());
        assertNull(raw.examples().getLast().movieTitle());
        assertEquals(raw.examples(), result.records().getFirst().inlineExamples());
    }

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
