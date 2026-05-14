package com.puzzlemovies.export.puzzlemovies;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.puzzlemovies.export.config.ExportProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PuzzleMoviesAuthClient {
    private final HttpClient httpClient;
    private final ExportProperties properties;
    private final ObjectMapper objectMapper;

    public PuzzleMoviesAuthClient(HttpClient httpClient, ExportProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PuzzleMoviesAuthResult authenticate(String email, String password) throws IOException, InterruptedException {
        String form = "email=" + encode(email) + "&password=" + encode(password);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getPuzzleMovies().getBaseUrl() + "/api2/user/signin"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return PuzzleMoviesAuthResult.failure("Authentication failed. Please try again.");
        }

        AuthResponse authResponse;
        try {
            authResponse = parseBody(response.body());
        } catch (IOException ex) {
            return PuzzleMoviesAuthResult.failure(
                    "Authentication could not be completed. Please re-register in Puzzle-Movies and try again.");
        }
        if (authResponse.error()) {
            return PuzzleMoviesAuthResult.failure(messageOrDefault(authResponse.message()));
        }

        if (authResponse.loggedInCookieName() == null || authResponse.loggedInCookieName().isBlank()
                || authResponse.loggedInCookie() == null || authResponse.loggedInCookie().isBlank()
                || authResponse.login() == null || authResponse.login().isBlank()) {
            return PuzzleMoviesAuthResult.failure("Authentication could not be completed. Please re-register in Puzzle-Movies and try again.");
        }

        List<String> setCookie = response.headers().allValues("set-cookie");
        if (setCookie.isEmpty()) {
            return PuzzleMoviesAuthResult.failure("Authentication could not be completed. Please re-register in Puzzle-Movies and try again.");
        }

        String cookieHeader = setCookie.stream()
                .map(value -> value.split(";", 2)[0])
                .collect(Collectors.joining("; "));
        return PuzzleMoviesAuthResult.success(cookieHeader);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private AuthResponse parseBody(String responseBody) throws IOException {
        return objectMapper.readValue(responseBody, AuthResponse.class);
    }

    private String messageOrDefault(String message) {
        if (message == null || message.isBlank()) {
            return "Authentication failed. Please try again.";
        }
        return message;
    }

    private record AuthResponse(boolean error,
                                String message,
                                String login,
                                @JsonProperty("logged_in_cookie_name") String loggedInCookieName,
                                @JsonProperty("logged_in_cookie") String loggedInCookie) {
        private AuthResponse() {
            this(false, null, null, null, null);
        }
    }
}
