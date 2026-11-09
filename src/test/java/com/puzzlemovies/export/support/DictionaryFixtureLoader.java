package com.puzzlemovies.export.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Offline source/expectation loading; deliberately independent of the production parser. */
public final class DictionaryFixtureLoader {
    private static final ObjectMapper JSON = new ObjectMapper();

    private DictionaryFixtureLoader() { }

    public static String html(String relativePath) {
        String path = relativePath.startsWith("/puzzlemovies/") ? relativePath
                : "/" + (relativePath.startsWith("puzzlemovies/") ? relativePath : "puzzlemovies/" + relativePath);
        try (InputStream input = DictionaryFixtureLoader.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalArgumentException("Missing dictionary fixture: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read dictionary fixture: " + path, ex);
        }
    }

    public static JsonNode json(String relativePath) {
        try {
            return JSON.readTree(html(relativePath));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid dictionary expectation JSON: " + relativePath, ex);
        }
    }

    public static List<ExpectedEntry> expectations(String relativePath) {
        try {
            return List.copyOf(JSON.readValue(html(relativePath), new TypeReference<List<ExpectedEntry>>() { }));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid dictionary expectations: " + relativePath, ex);
        }
    }

    public record ExpectedEntry(int position, String original, String translation,
                                String example, String exampleTranslation, String sourceLabel,
                                String movieTitle, String movieUrl) { }
}
