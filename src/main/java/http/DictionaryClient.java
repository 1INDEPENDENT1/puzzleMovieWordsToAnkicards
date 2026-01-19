package http;

import util.AppException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class DictionaryClient {
    private static final String BASE_URL = "https://puzzle-movies.com/dictionary";
    private static final int MAX_PAGES = 1000;

    public enum ItemType {
        WORD("word"),
        PHRASE("phrase");

        private final String value;

        ItemType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final HttpClient client;
    private final HttpClientProvider httpClientProvider;

    public DictionaryClient(HttpClient client, HttpClientProvider httpClientProvider) {
        this.client = client;
        this.httpClientProvider = httpClientProvider;
    }

    public List<String> fetchAllPages(String cookieHeader, ItemType itemType) {
        if (cookieHeader == null || cookieHeader.isBlank()) {
            throw AppException.auth("Cookie header is required to fetch dictionary.");
        }
        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            URI uri = URI.create(BASE_URL + "?item=" + itemType.getValue() + "&noredirect=&page=" + page);
            HttpRequest request = httpClientProvider.buildGet(uri, cookieHeader);
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw AppException.network("Failed to fetch dictionary page " + page, ex);
            }

            int status = response.statusCode();
            if (status == 302 || status == 401 || status == 403) {
                throw AppException.auth("Unauthorized dictionary access at page " + page + ".");
            }
            if (status != 200) {
                throw AppException.network("Dictionary fetch failed with status " + status + " on page " + page + ".");
            }

            String html = response.body();
            if (!hasRows(html)) {
                break;
            }
            pages.add(html);
        }
        return pages;
    }

    private boolean hasRows(String html) {
        if (html == null || html.isBlank()) {
            return false;
        }
        Document document = Jsoup.parse(html);
        Elements rows = document.select(".dictionary__row, .dictionary__item, .dictionary__list-item, table tr:has(td)");
        return !rows.isEmpty();
    }
}
