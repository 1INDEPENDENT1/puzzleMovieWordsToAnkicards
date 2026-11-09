package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryException;
import com.puzzlemovies.export.support.DictionaryFixtureLoader;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.puzzlemovies.export.export.DictionarySourceFieldsTest.*;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryParseAccountingTest {
    @Test void optionalWordFieldsDoNotRejectRecords() {
        var r = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/words-missing-optional-derived.html", 2)));
        assertEquals(2, r.records().size()); assertEquals(0, r.unparsedRecordCount());
        assertTrue(r.records().get(0).translations().isEmpty());
        assertTrue(r.acceptedSourceEntries().get(0).examples().isEmpty());
    }
    @Test void optionalPhraseFieldsDoNotRejectRecords() {
        var r = PARSER.parsePhrasePages(List.of(page(DictionarySection.PHRASE, "edge-cases/phrases-missing-optional-derived.html", 2)));
        assertEquals(2, r.records().size()); assertEquals(0, r.unparsedRecordCount());
        assertTrue(r.records().get(0).translations().isEmpty());
        assertNull(r.records().get(0).movieTitle()); assertNull(r.records().get(0).movieUrl());
        assertNull(r.acceptedSourceEntries().get(0).sourceLabel());
    }
    @Test void derivedInlineContextStaysSeparateWithoutInventedTranslationOrMetadata() {
        var r = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/words-inline-example-derived.html", 1)));
        assertEquals(1, r.records().size()); assertEquals("to mince", r.records().get(0).sourceText());
        assertEquals(List.of("смягчать"), List.copyOf(r.records().get(0).translations()));
        var example = r.acceptedSourceEntries().get(0).examples().get(0);
        assertEquals("You mince words, sometimes!", example.sourceText());
        assertTrue(example.translations().isEmpty()); assertNull(example.sourceLabel());
        assertNull(example.movieTitle()); assertNull(example.movieUrl());
    }
    @Test void inlineStructuredFieldsAreIndependentAndKeepSourceOnlyContext() {
        var doc = org.jsoup.Jsoup.parse(DictionaryFixtureLoader.html("edge-cases/words-inline-example-derived.html"));
        doc.selectFirst("td.dict__video__list-table__phrase").html("""
                <div class="puzzle-card__phrase-source">из субтитров</div>
                <a class="dict__video__list-table__info__balloon__title" href="/films/cars-3-2017">Тачки 3 / Cars 3</a>
                <div class="dict__video__list-table__phrase__eng"><span>You</span> mince <span>words</span>!</div>
                <div class="dict__video__list-table__phrase__rus">Вы говорите мягко!</div>
                """);
        var result = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, 1, doc.outerHtml(), 1)));
        assertEquals(1, result.records().size());
        var raw = result.acceptedSourceEntries().get(0);
        assertEquals("to mince", raw.sourceText()); assertEquals(List.of("смягчать"), raw.translations());
        var example = raw.examples().get(0);
        assertEquals("You mince words!", example.sourceText());
        assertEquals(List.of("Вы говорите мягко!"), example.translations());
        assertEquals("из субтитров", example.sourceLabel());
        assertEquals("Тачки 3 / Cars 3", example.movieTitle());
        assertEquals("https://puzzle-movies.com/films/cars-3-2017", example.movieUrl());
    }
    @Test void independentDuplicateRowsStayDistinctButSelectorOverlapDoesNotDuplicate() {
        var duplicates = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/words-duplicate-rows-derived.html", 2)));
        assertEquals(2, duplicates.records().size());
        assertEquals(duplicates.records().get(0).sourceText(), duplicates.records().get(1).sourceText());
        assertNotEquals(duplicates.acceptedSourceEntries().get(0).position(), duplicates.acceptedSourceEntries().get(1).position());
        var overlap = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/words-overlap-selectors-derived.html", 1)));
        assertEquals(1, overlap.records().size()); assertEquals(1, overlap.sourceRecordCount());
    }
    @Test void malformedWordAndPhraseOriginalsHaveExactPositionsBeforeDedupe() {
        var w = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/words-malformed-derived.html", 2)));
        var p = PARSER.parsePhrasePages(List.of(page(DictionarySection.PHRASE, "edge-cases/phrases-malformed-derived.html", 2)));
        for (var result : List.of(w, p)) {
            assertEquals(2, result.sourceRecordCount()); assertEquals(1, result.records().size());
            assertEquals(1, result.unparsedRecordCount());
            assertEquals(1, result.issues().get(0).position().rowNumber());
            assertEquals(ParseIssue.Reason.MISSING_ORIGINAL, result.issues().get(0).reason());
            assertEquals(2, result.acceptedSourceEntries().get(0).position().rowNumber());
        }
    }
    @Test void unusableIdentityIsCountedAndAllRejectedIsNotEmpty() {
        var html = DictionaryFixtureLoader.html("edge-cases/words-overlap-selectors-derived.html");
        var doc = org.jsoup.Jsoup.parse(html);
        doc.select(".translate_line_without_popup").html("!!!");
        var result = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, 1, doc.outerHtml(), 1)));
        assertEquals(1, result.sourceRecordCount()); assertEquals(1, result.unparsedRecordCount());
        assertTrue(result.records().isEmpty());
        assertEquals(ParseIssue.Reason.UNUSABLE_IDENTITY, result.issues().get(0).reason());
        var position = result.issues().get(0).position();
        var multiple = new DictionaryParseResult<>(List.of(), List.of(), List.of(new ParseIssue(position, ParseIssue.Reason.MISSING_ORIGINAL), new ParseIssue(position, ParseIssue.Reason.UNUSABLE_IDENTITY)), 1);
        assertEquals(1, multiple.unparsedRecordCount());
    }
    @Test void zeroShellIsEmptyButUnknownShapeFailsExplicitly() {
        assertEquals(0, PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "edge-cases/word-zero-derived.html", 0))).sourceRecordCount());
        assertEquals(0, PARSER.parsePhrasePages(List.of(page(DictionarySection.PHRASE, "edge-cases/phrase-zero-derived.html", 0))).sourceRecordCount());
        assertThrows(PuzzleMoviesDictionaryException.class, () -> PARSER.parseSourceEntries(page(DictionarySection.WORD, 1, "<table><tr><td>word</td></tr></table>", 0)));
        assertThrows(PuzzleMoviesDictionaryException.class, () -> PARSER.parseSourceEntries(page(DictionarySection.WORD, 1, DictionaryFixtureLoader.html("words-real.html"), 35)));
    }
    @Test void preservesOrderedPagesAndRejectsRepeatedPositions() {
        var first = page(DictionarySection.WORD, "edge-cases/words-overlap-selectors-derived.html", 1);
        var second = page(DictionarySection.WORD, 2, DictionaryFixtureLoader.html("edge-cases/words-missing-optional-derived.html"), 2);
        var result = PARSER.parseWordPages(List.of(first, second));
        assertEquals(3, result.sourceRecordCount());
        assertEquals(List.of(1, 2, 2), result.acceptedSourceEntries().stream().map(e -> e.position().pageNumber()).toList());
        assertThrows(PuzzleMoviesDictionaryException.class, () -> PARSER.parseWordPages(List.of(first, first)));
    }
}
