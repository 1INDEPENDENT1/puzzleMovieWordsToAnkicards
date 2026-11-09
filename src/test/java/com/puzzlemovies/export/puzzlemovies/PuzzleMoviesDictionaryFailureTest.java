package com.puzzlemovies.export.puzzlemovies;

import com.puzzlemovies.export.support.DictionaryHttpTestClient;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import static com.puzzlemovies.export.support.DictionaryFixtureLoader.html;
import static com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryClientTest.*;
import static org.junit.jupiter.api.Assertions.*;

class PuzzleMoviesDictionaryFailureTest {
    private static final String SECRET = "sentinel-private-cookie-and-password";
    @Test void rejects503OnFirstAndLaterRequiredPageWithSafeContext() {
        for (int page : new int[]{1, 2}) {
            var transport = new DictionaryHttpTestClient();
            if (page == 2) transport.enqueue(200, html("pages/word-derived-page-1.html"));
            transport.enqueue(503, SECRET);
            var failure = failure(transport);
            assertEquals(PuzzleMoviesDictionaryException.Category.HTTP, failure.category());
            assertEquals(DictionarySection.WORD, failure.section()); assertEquals(page, failure.pageNumber());
            assertEquals(503, failure.statusCode()); assertSafe(failure);
        }
    }
    @Test void networkMessageAndCauseDoNotLeak() {
        var failure = failure(new DictionaryHttpTestClient().enqueue(new IOException(SECRET)));
        assertEquals(PuzzleMoviesDictionaryException.Category.NETWORK, failure.category()); assertSafe(failure);
    }
    @Test void networkFailureOnLaterRequiredPageDoesNotReturnPartialPages() {
        var transport = new DictionaryHttpTestClient().enqueue(200, html("pages/word-derived-page-1.html"))
                .enqueue(new IOException(SECRET));
        var failure = failure(transport);
        assertEquals(PuzzleMoviesDictionaryException.Category.NETWORK, failure.category());
        assertEquals(2, failure.pageNumber());
    }
    @Test void restoresInterruptedFlagAndSanitizesFailure() {
        try {
            var failure = failure(new DictionaryHttpTestClient().enqueue(new InterruptedException(SECRET)));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(PuzzleMoviesDictionaryException.Category.INTERRUPTED, failure.category()); assertSafe(failure);
        } finally { Thread.interrupted(); }
    }
    @Test void blankNullAndUnknownBodyAreFailures() {
        for (String body : new String[]{"", "  ", null, "<h1>" + SECRET + "</h1>"}) {
            assertEquals(PuzzleMoviesDictionaryException.Category.STRUCTURE, failure(new DictionaryHttpTestClient().enqueue(200, body)).category());
        }
    }
    @Test void authOnLaterPageAnd403HaveDistinctCategories() {
        var transport = new DictionaryHttpTestClient().enqueue(200, html("pages/word-derived-page-1.html"))
                .enqueue(200, "<div class='movies-header__user-anon'>" + SECRET + "</div>");
        assertEquals(PuzzleMoviesDictionaryException.Category.AUTHENTICATION, failure(transport).category());
        assertEquals(PuzzleMoviesDictionaryException.Category.HTTP, failure(new DictionaryHttpTestClient().enqueue(403, SECRET)).category());
    }
    @Test void rejectsWordResponseForPhraseRequest() {
        var transport = new DictionaryHttpTestClient().enqueue(200, html("words-real.html"));
        var failure = assertThrows(PuzzleMoviesDictionaryException.class, () -> client(transport).fetchPhrasePages(SECRET));
        assertEquals(DictionarySection.PHRASE, failure.section()); assertSafe(failure);
    }
    @Test void rejectsChangedTotalAndRepeatedPage() {
        String first = html("pages/word-derived-page-1.html");
        for (String second : new String[]{html("pages/word-derived-page-2.html").replace(">6</span>", ">7</span>"), first}) {
            var failure = failure(new DictionaryHttpTestClient().enqueue(200, first).enqueue(200, second));
            assertTrue(failure.category() == PuzzleMoviesDictionaryException.Category.SOURCE_CHANGED || failure.category() == PuzzleMoviesDictionaryException.Category.PAGINATION);
        }
    }
    @Test void refusesCrossOriginContinuationAndRedirect() {
        String first = syntheticPage(1, 2, 2).replace("href=\"/dictionary?page=2\"", "href=\"https://other.example/dictionary?page=2\"");
        var transport = new DictionaryHttpTestClient().enqueue(200, first);
        assertEquals(PuzzleMoviesDictionaryException.Category.PAGINATION, failure(transport).category()); assertEquals(1, transport.requestCount());
        var redirected = new DictionaryHttpTestClient().enqueue(200, html("words-real.html"), URI.create("https://other.example/dictionary?item=word&page=1"));
        assertEquals(PuzzleMoviesDictionaryException.Category.PAGINATION, failure(redirected).category());
    }
    @Test void confirmedTerminalReturnsAvailableRowsWhenStableSourceCounterDisagrees() throws Exception {
        var transport = new DictionaryHttpTestClient().enqueue(200, syntheticPage(1, 2, null));
        var pages = client(transport).fetchWordPages(SECRET);
        assertEquals(1, pages.size());
        assertEquals(2, pages.get(0).inspection().declaredTotal());
        assertEquals(1, pages.get(0).inspection().sourceRecordCount());
        assertEquals(1, transport.requestCount());
    }
    private PuzzleMoviesDictionaryException failure(DictionaryHttpTestClient transport) {
        var failure = assertThrows(PuzzleMoviesDictionaryException.class, () -> client(transport).fetchWordPages(SECRET));
        assertSafe(failure); return failure;
    }
    private void assertSafe(Throwable failure) {
        assertFalse(failure.toString().contains(SECRET)); assertNull(failure.getCause());
        for (Throwable suppressed : failure.getSuppressed()) assertFalse(suppressed.toString().contains(SECRET));
    }
}
