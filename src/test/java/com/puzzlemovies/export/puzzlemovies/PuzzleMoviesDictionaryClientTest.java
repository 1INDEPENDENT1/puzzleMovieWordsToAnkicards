package com.puzzlemovies.export.puzzlemovies;

import com.puzzlemovies.export.config.ExportProperties;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PuzzleMoviesDictionaryClientTest {
    @Test
    void sendsCompleteCookieHeaderWhenFetchingWords() throws Exception {
        CapturingHttpClient httpClient = new CapturingHttpClient(200, """
                <html><body><table><tr><td>hello</td><td>translation</td></tr></table></body></html>
                """);
        PuzzleMoviesDictionaryClient client = new PuzzleMoviesDictionaryClient(httpClient, properties());
        String cookieHeader = "PHPSESSID=session; guest_id=guest; wp_logged_in_cookie=logged; wp_auth_cookie=auth";

        List<String> pages = client.fetchWordPages(cookieHeader);

        assertEquals(1, pages.size());
        assertEquals(cookieHeader, httpClient.lastRequest.headers().firstValue("Cookie").orElseThrow());
    }

    @Test
    void rejectsAnonymousHtmlReturnedWithOkStatus() {
        CapturingHttpClient httpClient = new CapturingHttpClient(200, """
                <div class="movies-header__user movies-header__user-anon">
                    <a href="javascript:void(0)" onclick="show_sign_popup();">
                        <div class="movies-header__user-enter"><span>Sign in</span></div>
                    </a>
                </div>
                """);
        PuzzleMoviesDictionaryClient client = new PuzzleMoviesDictionaryClient(httpClient, properties());

        assertThrows(PuzzleMoviesSessionExpiredException.class,
                () -> client.fetchPhrasePages("wp_logged_in_cookie=stale"));
    }

    private ExportProperties properties() {
        ExportProperties properties = new ExportProperties();
        properties.getPuzzleMovies().setBaseUrl("https://puzzle-movies.com");
        return properties;
    }

    private static final class CapturingHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;
        private HttpRequest lastRequest;

        private CapturingHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NORMAL;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            this.lastRequest = request;
            return new StubHttpResponse<>((T) body, statusCode, request);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private record StubHttpResponse<T>(T body, int statusCode, HttpRequest request) implements HttpResponse<T> {
        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
