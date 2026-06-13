package com.puzzlemovies.export.export;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record PhraseExample(
        String wordIdentity,
        String phraseSourceText,
        Set<String> phraseTranslations,
        String movieTitle,
        String movieUrl) {

    public PhraseExample {
        if (wordIdentity == null || wordIdentity.isBlank()) {
            throw new IllegalArgumentException("wordIdentity must be non-blank");
        }
        if (phraseSourceText == null || phraseSourceText.isBlank()) {
            throw new IllegalArgumentException("phraseSourceText must be non-blank");
        }
        wordIdentity = wordIdentity.trim();
        phraseSourceText = phraseSourceText.trim();
        phraseTranslations = orderedCopy(phraseTranslations);
        movieTitle = blankToNull(movieTitle);
        movieUrl = blankToNull(movieUrl);
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
