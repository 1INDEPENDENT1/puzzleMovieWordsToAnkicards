package model;

import java.util.List;

public record DictionaryPhrase(
        String english,
        String russian,
        String movieTitle,
        String movieUrl,
        List<String> normalizedTokens
) {
    public static DictionaryPhrase fromRaw(String english, String russian, String movieTitle, String movieUrl) {
        String cleanedEnglish = normalizeText(english);
        String cleanedRussian = normalizeNullable(russian);
        String cleanedTitle = normalizeNullable(movieTitle);
        String cleanedUrl = normalizeNullable(movieUrl);
        List<String> tokens = Tokenizer.normalizeTokens(cleanedEnglish);
        return new DictionaryPhrase(cleanedEnglish, cleanedRussian, cleanedTitle, cleanedUrl, tokens);
    }

    private static String normalizeText(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static final class Tokenizer {
        private static List<String> normalizeTokens(String text) {
            if (text == null || text.isBlank()) {
                return List.of();
            }
            String normalized = text.toLowerCase()
                    .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                    .trim();
            if (normalized.isBlank()) {
                return List.of();
            }
            return List.of(normalized.split("\\s+"));
        }
    }
}
