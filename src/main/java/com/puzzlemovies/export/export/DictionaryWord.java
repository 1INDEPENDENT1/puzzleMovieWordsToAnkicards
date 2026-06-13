package com.puzzlemovies.export.export;

import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.Set;

public record DictionaryWord(
        String sourceText,
        Set<String> translations,
        String matchingIdentity,
        boolean normalizationFallbackUsed) {

    public DictionaryWord {
        sourceText = requireText(sourceText, "sourceText");
        matchingIdentity = requireText(matchingIdentity, "matchingIdentity");
        translations = orderedCopy(translations);
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
}
