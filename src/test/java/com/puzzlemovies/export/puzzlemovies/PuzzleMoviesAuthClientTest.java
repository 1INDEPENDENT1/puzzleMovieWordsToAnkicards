package com.puzzlemovies.export.puzzlemovies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puzzlemovies.export.config.ExportProperties;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleMoviesAuthClientTest {
    @Test
    void returnsFailureMessageFromResponseBody() throws Exception {
        PuzzleMoviesAuthClient client = new PuzzleMoviesAuthClient(
                new StubHttpClient("""
                        {
                          "error" : true,
                          "key" : "password",
                          "message" : "Неверный пароль"
                        }
                        """, List.of("wp_logged_in_cookie=bad")),
                properties(),
                new ObjectMapper());

        PuzzleMoviesAuthResult result = client.authenticate("user@example.com", "wrong");

        assertFalse(result.isSuccess());
        assertEquals("Неверный пароль", result.errorMessage().orElseThrow());
    }

    @Test
    void requiresExpectedSuccessFields() throws Exception {
        PuzzleMoviesAuthClient client = new PuzzleMoviesAuthClient(
                new StubHttpClient("""
                        {"error":false,"user_id":100372848,"login":"miknsts@mail.ru","logged_in_cookie_name":"wp_logged_in_cookie","logged_in_cookie":"miknsts@mail.ru|2093862556|eYzms3twXkebQ2iboASV3MonircgNRq933oN6BqPV3Z"}
                        """, List.of("wp_logged_in_cookie=value", "wordpress_test_cookie=1")),
                properties(),
                new ObjectMapper());

        PuzzleMoviesAuthResult result = client.authenticate("user@example.com", "correct");

        assertTrue(result.isSuccess());
        assertEquals("wp_logged_in_cookie=value; wordpress_test_cookie=1", result.cookieHeader().orElseThrow());
    }

    @Test
    void rejectsBodyMissingLoggedInCookieData() throws Exception {
        PuzzleMoviesAuthClient client = new PuzzleMoviesAuthClient(
                new StubHttpClient("""
                        {"error":false,"user_id":100372848,"login":"miknsts@mail.ru"}
                        """, List.of("wp_logged_in_cookie=value")),
                properties(),
                new ObjectMapper());

        PuzzleMoviesAuthResult result = client.authenticate("user@example.com", "wrong-shape");

        assertFalse(result.isSuccess());
        assertEquals(
                "Authentication could not be completed. Please re-register in Puzzle-Movies and try again.",
                result.errorMessage().orElseThrow());
    }

    private ExportProperties properties() {
        ExportProperties properties = new ExportProperties();
        properties.setOutputDir("exports");
        properties.getPuzzleMovies().setBaseUrl("https://puzzle-movies.com");
        return properties;
    }

    private static final class StubHttpClient extends HttpClient {
        private final String body;
        private final List<String> cookies;

        private StubHttpClient(String body, List<String> cookies) {
            this.body = body;
            this.cookies = cookies;
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
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return new StubHttpResponse<>((T) body, cookies);
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

    private record StubHttpResponse<T>(T body, List<String> cookies) implements HttpResponse<T> {
        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of("set-cookie", cookies), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("https://puzzle-movies.com/api2/user/signin");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
