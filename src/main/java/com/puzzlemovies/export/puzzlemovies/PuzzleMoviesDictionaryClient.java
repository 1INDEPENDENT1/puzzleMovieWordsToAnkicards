package com.puzzlemovies.export.puzzlemovies;

import com.puzzlemovies.export.config.ExportProperties;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryException.Category.*;

@Component
public class PuzzleMoviesDictionaryClient {
    private final HttpClient httpClient;
    private final ExportProperties properties;
    private final DictionaryPageInspector inspector = new DictionaryPageInspector();

    public PuzzleMoviesDictionaryClient(HttpClient httpClient, ExportProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }
    public List<DictionaryPage> fetchWordPages(String cookieHeader) throws IOException, InterruptedException {
        return fetchPages(DictionarySection.WORD, cookieHeader);
    }
    public List<DictionaryPage> fetchPhrasePages(String cookieHeader) throws IOException, InterruptedException {
        return fetchPages(DictionarySection.PHRASE, cookieHeader);
    }
    private List<DictionaryPage> fetchPages(DictionarySection section, String cookieHeader) {
        List<DictionaryPage> pages = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Integer declaredTotal = null;
        int page = 1;
        URI origin = URI.create(properties.getPuzzleMovies().getBaseUrl());
        while (true) {
            if (!visited.add(page)) throw failure(PAGINATION, section, page, null);
            URI uri = origin.resolve("/dictionary?item=" + section.item() + "&noredirect=&page=" + page);
            HttpRequest request = HttpRequest.newBuilder(uri).header("Cookie", cookieHeader).header("Accept", "text/html").GET().build();
            HttpResponse<String> response;
            try { response = httpClient.send(request, HttpResponse.BodyHandlers.ofString()); }
            catch (InterruptedException ex) {
                Thread.currentThread().interrupt(); throw failure(INTERRUPTED, section, page, null);
            } catch (IOException ex) { throw failure(NETWORK, section, page, null); }
            if (response.statusCode() != 200) throw failure(HTTP, section, page, response.statusCode());
            URI actual = response.uri();
            if (actual == null || !DictionaryPageInspector.sameOrigin(uri, actual)) throw failure(PAGINATION, section, page, null);
            if (!"/dictionary".equals(actual.getPath())) {
                if (actual.getPath() != null && (actual.getPath().contains("login") || actual.getPath().contains("signin")))
                    throw new PuzzleMoviesSessionExpiredException(section, page);
                throw failure(PAGINATION, section, page, null);
            }
            String body = response.body();
            var inspection = inspector.inspect(section, page, uri, body);
            if (declaredTotal == null) declaredTotal = inspection.declaredTotal();
            else if (declaredTotal != inspection.declaredTotal()) throw failure(SOURCE_CHANGED, section, page, null);
            pages.add(new DictionaryPage(section, page, uri, body, inspection));
            if (inspection.nextPage() == null) {
                return List.copyOf(pages);
            }
            if (inspection.nextPage() != page + 1) throw failure(PAGINATION, section, page, null);
            page = inspection.nextPage();
        }
    }
    private PuzzleMoviesDictionaryException failure(PuzzleMoviesDictionaryException.Category category, DictionarySection section, int page, Integer status) {
        return new PuzzleMoviesDictionaryException(category, section, page, status);
    }
}
