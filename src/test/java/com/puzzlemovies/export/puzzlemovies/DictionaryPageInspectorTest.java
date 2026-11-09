package com.puzzlemovies.export.puzzlemovies;

import org.junit.jupiter.api.Test;
import static com.puzzlemovies.export.support.DictionaryFixtureLoader.html;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryPageInspectorTest {
    private final DictionaryPageInspector inspector = new DictionaryPageInspector();

    @Test void inspectsObservedWordShellAndRawRowsWithoutFieldParsing() {
        var page = inspector.inspect(DictionarySection.WORD, 1, html("words-real.html"));
        assertEquals(DictionarySection.WORD, page.section());
        assertEquals(1489, page.declaredTotal());
        assertEquals(36, page.sourceRecordCount());
        assertEquals(1, page.currentPage());
        assertEquals(2, page.nextPage());
        assertTrue(page.recognized()); assertTrue(page.authenticated());
    }
    @Test void inspectsObservedPhraseShellSeparately() {
        var page = inspector.inspect(DictionarySection.PHRASE, 1, html("phrases-real.html"));
        assertEquals(766, page.declaredTotal()); assertEquals(36, page.sourceRecordCount()); assertEquals(2, page.nextPage());
    }
    @Test void observedLastPagesAreTerminalDespiteUnrelatedPageSubstring() {
        var word = inspector.inspect(DictionarySection.WORD, 42, html("pages/word-real-page-42.html") + "<script>var unrelated='page=43';</script>");
        assertEquals(13, word.sourceRecordCount()); assertNull(word.nextPage());
        var phrase = inspector.inspect(DictionarySection.PHRASE, 22, html("pages/phrase-real-page-22.html"));
        assertEquals(10, phrase.sourceRecordCount()); assertNull(phrase.nextPage());
    }
    @Test void approvedDerivedZeroHasKnownShell() {
        var page = inspector.inspect(DictionarySection.WORD, 1, html("edge-cases/word-zero-derived.html"));
        assertEquals(0, page.declaredTotal()); assertEquals(0, page.sourceRecordCount()); assertNull(page.nextPage());
    }
    @Test void countsRowsEvenWhenOriginalIsMalformed() {
        var page = inspector.inspect(DictionarySection.WORD, 1, html("edge-cases/words-malformed-derived.html"));
        assertEquals(2, page.sourceRecordCount());
    }
    @Test void arbitraryTableIsNotDictionaryShell() {
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.WORD, 1, "<table><tr><td>word</td></tr></table>"));
    }
    @Test void positiveTotalWithZeroRowsIsFailure() {
        String empty = html("edge-cases/word-zero-derived.html").replace(">0</span>", ">8</span>");
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.WORD, 1, empty));
    }
    @Test void wrongSectionAndRepeatedCurrentPageFail() {
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.PHRASE, 1, html("words-real.html")));
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.WORD, 2, html("words-real.html")));
    }
    @Test void rejectsGapAndBackwardsNextArrow() {
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.WORD, 1, PuzzleMoviesDictionaryClientTest.syntheticPage(1, 3, 3)));
        assertThrows(PuzzleMoviesDictionaryException.class, () -> inspector.inspect(DictionarySection.WORD, 2, PuzzleMoviesDictionaryClientTest.syntheticPage(2, 3, 1)));
    }
    @Test void signedOutShellUsesCompatibleSessionException() {
        var failure = assertThrows(PuzzleMoviesSessionExpiredException.class,
                () -> inspector.inspect(DictionarySection.PHRASE, 2, "<div class='movies-header__user-anon'>Sign in</div>"));
        assertEquals(PuzzleMoviesDictionaryException.Category.AUTHENTICATION, failure.category());
        assertEquals(2, failure.pageNumber());
    }
}
