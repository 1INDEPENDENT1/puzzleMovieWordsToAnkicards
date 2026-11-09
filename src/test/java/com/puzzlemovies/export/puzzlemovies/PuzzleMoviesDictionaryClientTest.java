package com.puzzlemovies.export.puzzlemovies;

import com.puzzlemovies.export.config.ExportProperties;
import com.puzzlemovies.export.support.DictionaryHttpTestClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static com.puzzlemovies.export.support.DictionaryFixtureLoader.html;
import static org.junit.jupiter.api.Assertions.*;

class PuzzleMoviesDictionaryClientTest {
    @Test
    void fetchesEveryDerivedWordPageInOrderAndStopsAtTerminal() throws Exception {
        DictionaryHttpTestClient transport = sequence("word");
        var pages = client(transport).fetchWordPages("test-cookie");
        assertEquals(3, pages.size());
        assertEquals(List.of(
                URI.create("https://puzzle-movies.com/dictionary?item=word&noredirect=&page=1"),
                URI.create("https://puzzle-movies.com/dictionary?item=word&noredirect=&page=2"),
                URI.create("https://puzzle-movies.com/dictionary?item=word&noredirect=&page=3")), transport.requestedUris());
        assertEquals(0, transport.remainingResponses());
    }

    @Test
    void explicitlySelectsPhrasesForEveryPage() throws Exception {
        DictionaryHttpTestClient transport = sequence("phrase");
        assertEquals(3, client(transport).fetchPhrasePages("test-cookie").size());
        assertEquals("item=phrase&noredirect=&page=3", transport.requestedUris().get(2).getRawQuery());
    }

    @Test
    void traversesFiftyOnePagesWithoutSilentTruncation() throws Exception {
        DictionaryHttpTestClient transport = new DictionaryHttpTestClient();
        for (int page = 1; page <= 51; page++) transport.enqueue(200, syntheticPage(page, 51, page < 51 ? page + 1 : null));
        assertEquals(51, client(transport).fetchWordPages("test-cookie").size());
        assertEquals(51, transport.requestCount());
        assertEquals(0, transport.remainingResponses());
    }

    @Test
    void acceptsApprovedSyntheticEmptyShellWithoutAnotherRequest() throws Exception {
        DictionaryHttpTestClient transport = new DictionaryHttpTestClient().enqueue(200, html("edge-cases/word-zero-derived.html"));
        assertEquals(1, client(transport).fetchWordPages("test-cookie").size());
        assertEquals(1, transport.requestCount());
    }

    @Test void stableUnderstatedCounterDoesNotStopStructuralContinuation() throws Exception {
        for (int total : List.of(0, 1)) {
            var transport = new DictionaryHttpTestClient().enqueue(200, syntheticPage(1, total, 2))
                    .enqueue(200, syntheticPage(2, total, null));
            assertEquals(2, client(transport).fetchWordPages("FAKE_TEST_ONLY").size());
            assertEquals(2, transport.requestCount());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesCompleteCookieHeaderOnAuthenticatedRequest() throws Exception {
        var transport = org.mockito.Mockito.mock(java.net.http.HttpClient.class);
        var response = org.mockito.Mockito.mock(java.net.http.HttpResponse.class);
        org.mockito.Mockito.when(response.statusCode()).thenReturn(200);
        org.mockito.Mockito.when(response.body()).thenReturn(html("edge-cases/word-zero-derived.html"));
        org.mockito.Mockito.when(response.uri()).thenReturn(URI.create("https://puzzle-movies.com/dictionary?item=word&noredirect=&page=1"));
        String cookie = "TEST_SESSION=one; TEST_SECOND=two";
        org.mockito.Mockito.when(transport.send(org.mockito.ArgumentMatchers.any(java.net.http.HttpRequest.class),
                org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenAnswer(call -> {
            java.net.http.HttpRequest request = call.getArgument(0);
            assertEquals(cookie, request.headers().firstValue("Cookie").orElseThrow());
            return response;
        });
        ExportProperties properties = new ExportProperties();
        properties.getPuzzleMovies().setBaseUrl("https://puzzle-movies.com");
        assertEquals(1, new PuzzleMoviesDictionaryClient(transport, properties).fetchWordPages(cookie).size());
    }

    static DictionaryHttpTestClient sequence(String section) {
        DictionaryHttpTestClient transport = new DictionaryHttpTestClient();
        for (int page = 1; page <= 3; page++) transport.enqueue(200, html("pages/" + section + "-derived-page-" + page + ".html"));
        return transport;
    }

    /** Explicit synthetic boundary based on the sanitized observed shell, never a claimed live page. */
    static String syntheticPage(int page, int total, Integer next) {
        Document doc = Jsoup.parse(html("pages/word-derived-page-1.html"));
        doc.select(".movies-tabs__item.is-active span").first().text(Integer.toString(total));
        var rows = doc.select("table.dict__video__list-table tr.dict__video__list-table__row");
        rows.get(1).remove();
        var list = doc.selectFirst(".paginator-style-2__list");
        list.empty().append("<li class='paginator-style-2__element is-current'>" + page + "</li>");
        if (page > 1) list.append("<li class='paginator-style-2__element'><a href='/dictionary?page=" + (page-1) + "'>Previous</a></li>");
        if (next != null) list.append("<li class='paginator-style-2__element paginator-style-2__right'><a href='/dictionary?page=" + next + "'>Next</a></li>");
        return doc.outerHtml();
    }

    static PuzzleMoviesDictionaryClient client(DictionaryHttpTestClient transport) {
        ExportProperties properties = new ExportProperties();
        properties.getPuzzleMovies().setBaseUrl("https://puzzle-movies.com");
        return new PuzzleMoviesDictionaryClient(transport, properties);
    }
}
