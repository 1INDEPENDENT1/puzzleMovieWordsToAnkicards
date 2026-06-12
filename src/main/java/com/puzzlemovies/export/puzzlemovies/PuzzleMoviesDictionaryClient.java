package com.puzzlemovies.export.puzzlemovies;

import com.puzzlemovies.export.config.ExportProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class PuzzleMoviesDictionaryClient {
    private static final int MAX_PAGES = 50;

    private final HttpClient httpClient;
    private final ExportProperties properties;

    public PuzzleMoviesDictionaryClient(HttpClient httpClient, ExportProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public List<String> fetchWordPages(String cookieHeader) throws IOException, InterruptedException {
        return fetchPages("/dictionary/words", cookieHeader);
    }

    public List<String> fetchPhrasePages(String cookieHeader) throws IOException, InterruptedException {
        return fetchPages("/dictionary/phrases", cookieHeader);
    }

    private List<String> fetchPages(String path, String cookieHeader) throws IOException, InterruptedException {
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = properties.getPuzzleMovies().getBaseUrl() + path + "?page=" + page;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Cookie", cookieHeader)
                    .header("Accept", "text/html")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                break;
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                break;
            }
            if (isAnonymousPage(body)) {
                throw new PuzzleMoviesSessionExpiredException();
            }
            pages.add(body);

            if (!body.contains("page=" + (page + 1))) {
                break;
            }
        }
        return pages;
    }

    private boolean isAnonymousPage(String body) {
        return body.contains("movies-header__user-anon")
                || body.contains("show_sign_popup()");
    }
}
