package com.puzzlemovies.export.export;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record DictionaryPhrase(
        String sourceText,
        Set<String> translations,
        String movieTitle,
        String movieUrl,
        Set<String> matchingTokens,
        String dedupeIdentity,
        boolean normalizationFallbackUsed) {

    public DictionaryPhrase {
        sourceText = requireText(sourceText, "sourceText");
        dedupeIdentity = requireText(dedupeIdentity, "dedupeIdentity");
        translations = orderedCopy(translations);
        matchingTokens = orderedCopy(matchingTokens);
        movieTitle = blankToNull(movieTitle);
        movieUrl = blankToNull(movieUrl);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value.trim();
    }

    private static Set<String> orderedCopy(Set<String> values) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(copy::add);
        }
        return Collections.unmodifiableSet(copy);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
