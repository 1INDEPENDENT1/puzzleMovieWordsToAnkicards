package model;

public record DictionaryWord(String english, String russian, String normalizedKey) {
    public static DictionaryWord fromRaw(String english, String russian) {
        String cleanedEnglish = normalizeText(english);
        String cleanedRussian = normalizeNullable(russian);
        return new DictionaryWord(cleanedEnglish, cleanedRussian, normalizeKey(cleanedEnglish));
    }

    private static String normalizeKey(String english) {
        if (english == null) {
            return "";
        }
        return english.toLowerCase()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "");
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
}
