package com.puzzlemovies.export.puzzlemovies;

import java.util.Optional;

public record PuzzleMoviesAuthResult(String cookieHeaderValue, String errorMessageValue) {
    public static PuzzleMoviesAuthResult success(String cookieHeader) {
        return new PuzzleMoviesAuthResult(cookieHeader, null);
    }

    public static PuzzleMoviesAuthResult failure(String errorMessage) {
        return new PuzzleMoviesAuthResult(null, errorMessage);
    }

    public boolean isSuccess() {
        return cookieHeaderValue != null && !cookieHeaderValue.isBlank();
    }

    public Optional<String> cookieHeader() {
        return Optional.ofNullable(cookieHeaderValue);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessageValue);
    }
}
