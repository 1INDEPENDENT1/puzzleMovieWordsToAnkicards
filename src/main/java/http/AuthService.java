package http;

import util.AppException;
import util.TokenStore;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AuthService {
    private static final URI BASE_URI = URI.create("https://puzzle-movies.com");
    private static final URI SIGN_IN_URI = URI.create("https://puzzle-movies.com/api2/user/signin");

    private final HttpClient client;
    private final HttpClientProvider httpClientProvider;

    public AuthService(HttpClient client, HttpClientProvider httpClientProvider) {
        this.client = client;
        this.httpClientProvider = httpClientProvider;
    }

    public String authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw AppException.input("Email and password are required.");
        }
        String guestCookie = fetchGuestCookies();
        String authCookie = signIn(email, password, guestCookie);
        return mergeCookies(guestCookie, authCookie);
    }

    private String fetchGuestCookies() {
        HttpRequest request = httpClientProvider.buildGet(BASE_URI, null);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw AppException.auth("Guest session failed with status " + response.statusCode());
            }
            String cookieHeader = cookiesFromHeaders(response.headers());
            return TokenStore.normalizeCookieHeader(cookieHeader);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.network("Failed to obtain guest cookies.", ex);
        }
    }

    private String signIn(String email, String password, String guestCookie) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("email", email);
        form.put("password", password);
        form.put("cookie", guestCookie);

        String body = form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));

        HttpRequest request = httpClientProvider.buildPostForm(SIGN_IN_URI, body, guestCookie);
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw AppException.auth("Sign-in failed with status " + response.statusCode());
            }
            String cookieHeader = cookiesFromHeaders(response.headers());
            return TokenStore.normalizeCookieHeader(cookieHeader);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw AppException.network("Failed to authenticate.", ex);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String cookiesFromHeaders(HttpHeaders headers) {
        List<String> cookies = headers.allValues("set-cookie");
        if (cookies == null || cookies.isEmpty()) {
            throw AppException.auth("No cookies returned by server.");
        }
        return cookies.stream()
                .map(AuthService::cookiePair)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("; "));
    }

    private static String cookiePair(String setCookieHeader) {
        String[] parts = setCookieHeader.split(";", 2);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    private static String mergeCookies(String first, String second) {
        Map<String, String> merged = new LinkedHashMap<>();
        addCookieHeader(merged, first);
        addCookieHeader(merged, second);
        return merged.values().stream()
                .collect(Collectors.joining("; "));
    }

    private static void addCookieHeader(Map<String, String> jar, String header) {
        if (header == null || header.isBlank()) {
            return;
        }
        String[] pairs = header.split(";");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String name = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if (!name.isEmpty()) {
                jar.put(name.toLowerCase(Locale.ROOT), name + "=" + value);
            }
        }
    }
}
