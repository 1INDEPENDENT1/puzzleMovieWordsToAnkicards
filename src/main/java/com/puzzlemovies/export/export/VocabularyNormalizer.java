package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class VocabularyNormalizer {
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$");
    private static final Pattern INNER_PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}']+");

    private final Lemmatizer lemmatizer;

    public VocabularyNormalizer(Lemmatizer lemmatizer) {
        this.lemmatizer = lemmatizer;
    }

    public NormalizedText normalizeSingle(String text) {
        List<NormalizedToken> tokens = normalizeTokens(text);
        if (tokens.isEmpty()) {
            String fallback = cleanToken(text);
            return new NormalizedText(fallback, fallback.isBlank() ? Set.of() : orderedSet(fallback), true);
        }
        String identity = tokens.get(0).value();
        boolean fallbackUsed = tokens.stream().anyMatch(NormalizedToken::fallbackUsed);
        return new NormalizedText(identity, orderedValues(tokens), fallbackUsed);
    }

    public NormalizedText normalizePhrase(String text) {
        List<NormalizedToken> tokens = normalizeTokens(text);
        Set<String> values = orderedValues(tokens);
        String identity = String.join(" ", values);
        boolean fallbackUsed = tokens.stream().anyMatch(NormalizedToken::fallbackUsed);
        if (identity.isBlank()) {
            identity = cleanToken(text);
            if (!identity.isBlank()) {
                values = orderedSet(identity);
            }
            fallbackUsed = true;
        }
        return new NormalizedText(identity, values, fallbackUsed);
    }

    private List<NormalizedToken> normalizeTokens(String text) {
        List<String> sourceTokens = tokenize(text);
        List<NormalizedToken> normalized = new ArrayList<>();
        for (String sourceToken : sourceTokens) {
            String cleaned = cleanToken(sourceToken);
            if (cleaned.isBlank()) {
                continue;
            }
            try {
                String lemma = cleanToken(lemmatizer.lemma(cleaned));
                if (!lemma.isBlank()) {
                    normalized.add(new NormalizedToken(lemma, false));
                    continue;
                }
            } catch (RuntimeException ignored) {
                // Fall back below; normalization must not fail a full export.
            }
            normalized.add(new NormalizedToken(cleaned, true));
        }
        return normalized;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        try {
            List<String> tokens = lemmatizer.tokenize(text);
            if (tokens != null && !tokens.isEmpty()) {
                return tokens;
            }
        } catch (RuntimeException ignored) {
            // Use simple tokenization fallback below.
        }
        return List.of(INNER_PUNCTUATION.matcher(text).replaceAll(" ").split("\\s+"));
    }

    private Set<String> orderedValues(List<NormalizedToken> tokens) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (NormalizedToken token : tokens) {
            values.add(token.value());
        }
        return Collections.unmodifiableSet(values);
    }

    private Set<String> orderedSet(String value) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add(value);
        return Collections.unmodifiableSet(values);
    }

    private String cleanToken(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT).trim();
        lowered = EDGE_PUNCTUATION.matcher(lowered).replaceAll("");
        lowered = INNER_PUNCTUATION.matcher(lowered).replaceAll("");
        return lowered.trim();
    }

    private record NormalizedToken(String value, boolean fallbackUsed) {
    }

    public record NormalizedText(String identity, Set<String> tokens, boolean fallbackUsed) {
    }
}
