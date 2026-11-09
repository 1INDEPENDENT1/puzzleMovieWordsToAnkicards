package com.puzzlemovies.export.support;

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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Queued String-body transport. Captures URIs only, never cookie or other request headers. */
public final class DictionaryHttpTestClient extends HttpClient {
    private final Deque<Object> queue = new ArrayDeque<>();
    private final List<URI> requestedUris = new ArrayList<>();

    public DictionaryHttpTestClient enqueue(int status, String body) {
        return enqueue(status, body, null);
    }

    public DictionaryHttpTestClient enqueue(int status, String body, URI responseUri) {
        queue.addLast(new QueuedResponse(status, body, responseUri));
        return this;
    }

    public DictionaryHttpTestClient enqueue(IOException failure) { queue.addLast(failure); return this; }
    public DictionaryHttpTestClient enqueue(InterruptedException failure) { queue.addLast(failure); return this; }
    public List<URI> requestedUris() { return List.copyOf(requestedUris); }
    public int requestCount() { return requestedUris.size(); }
    public int remainingResponses() { return queue.size(); }

    @Override
    @SuppressWarnings("unchecked")
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        requestedUris.add(request.uri());
        if (queue.isEmpty()) throw new AssertionError("No queued dictionary response for request " + requestCount());
        Object item = queue.removeFirst();
        if (item instanceof IOException failure) throw failure;
        if (item instanceof InterruptedException failure) throw failure;
        QueuedResponse response = (QueuedResponse) item;
        URI uri = response.uri() == null ? request.uri() : response.uri();
        // Retain a safe stand-in request rather than the caller's cookie-bearing request.
        HttpRequest safeRequest = HttpRequest.newBuilder(request.uri()).GET().build();
        return (HttpResponse<T>) new StringResponse(response.status(), response.body(), uri, safeRequest);
    }

    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        try { return CompletableFuture.completedFuture(send(request, handler)); }
        catch (IOException | InterruptedException ex) { return CompletableFuture.failedFuture(ex); }
    }
    @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> handler,
            HttpResponse.PushPromiseHandler<T> pushHandler) { return sendAsync(request, handler); }
    @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
    @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
    @Override public Redirect followRedirects() { return Redirect.NORMAL; }
    @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
    @Override public SSLContext sslContext() {
        try { return SSLContext.getDefault(); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Override public SSLParameters sslParameters() { return new SSLParameters(); }
    @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
    @Override public Version version() { return Version.HTTP_1_1; }
    @Override public Optional<Executor> executor() { return Optional.empty(); }

    private record QueuedResponse(int status, String body, URI uri) { }
    private record StringResponse(int statusCode, String body, URI uri, HttpRequest request) implements HttpResponse<String> {
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (name, value) -> true); }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
    }
}
