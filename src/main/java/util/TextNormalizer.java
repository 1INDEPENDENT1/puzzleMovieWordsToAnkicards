package util;

import java.util.List;
import java.util.Locale;

public class TextNormalizer {
    public List<String> normalizeTokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = normalizeText(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\s+"));
    }

    public String normalizeWord(String word) {
        if (word == null || word.isBlank()) {
            return "";
        }
        return normalizeText(word).replaceAll("\\s+", "");
    }

    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .trim();
    }
}
