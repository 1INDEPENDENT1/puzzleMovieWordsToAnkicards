package http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

public class HttpClientProvider {
    private static final String USER_AGENT = "PuzzleMoviesExporter/1.0";

    public HttpClient create() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", USER_AGENT);
    }

    public HttpRequest buildGet(URI uri, String cookieHeader) {
        HttpRequest.Builder builder = baseRequest(uri).GET();
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            builder.header("Cookie", cookieHeader);
        }
        return builder.build();
    }

    public HttpRequest buildPostForm(URI uri, String formBody, String cookieHeader) {
        HttpRequest.Builder builder = baseRequest(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            builder.header("Cookie", cookieHeader);
        }
        return builder.build();
    }
}
