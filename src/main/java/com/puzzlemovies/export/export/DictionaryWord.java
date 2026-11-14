package com.puzzlemovies.export.export;

import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public record DictionaryWord(
        String sourceText,
        Set<String> translations,
        String matchingIdentity,
        boolean normalizationFallbackUsed,
        List<RawDictionaryEntry> sources) {

    public DictionaryWord(String sourceText, Set<String> translations, String matchingIdentity,
                          boolean normalizationFallbackUsed) {
        this(sourceText, translations, matchingIdentity, normalizationFallbackUsed, List.of());
    }

    public DictionaryWord {
        sourceText = requireText(sourceText, "sourceText");
        matchingIdentity = requireText(matchingIdentity, "matchingIdentity");
        translations = orderedCopy(translations);
        sources = List.copyOf(sources);
    }

    public String sourceIdentity() {
        return VocabularyNormalizer.canonicalSourceText(sourceText);
    }

    public List<SourceExample> inlineExamples() {
        return sources.stream().flatMap(source -> source.examples().stream()).toList();
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
