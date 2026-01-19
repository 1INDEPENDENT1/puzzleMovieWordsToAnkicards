package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class TokenStore {
    public Optional<String> readToken(Path tokenFile) {
        if (tokenFile == null || !Files.exists(tokenFile)) {
            return Optional.empty();
        }
        try {
            String value = Files.readString(tokenFile, StandardCharsets.UTF_8).trim();
            if (value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (IOException ex) {
            throw AppException.io("Failed to read token file: " + tokenFile, ex);
        }
    }

    public void writeToken(Path tokenFile, String cookieHeader) {
        if (tokenFile == null) {
            throw AppException.input("Token file path is required.");
        }
        String normalized = normalizeCookieHeader(cookieHeader);
        try {
            Path parent = tokenFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(tokenFile, normalized + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw AppException.io("Failed to write token file: " + tokenFile, ex);
        }
    }

    public static String normalizeCookieHeader(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            throw AppException.auth("Cookie header is empty.");
        }
        String normalized = cookieHeader.trim();
        if (!normalized.contains("=")) {
            throw AppException.auth("Cookie header is invalid.");
        }
        return normalized;
    }
}
