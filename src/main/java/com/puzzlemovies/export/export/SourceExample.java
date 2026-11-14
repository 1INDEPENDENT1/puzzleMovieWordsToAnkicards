package com.puzzlemovies.export.export;
import java.util.List;
public record SourceExample(String sourceText, List<String> translations, String sourceLabel, String movieTitle, String movieUrl,
                            String stableSourceId) {
    public SourceExample(String sourceText, List<String> translations, String sourceLabel, String movieTitle, String movieUrl) {
        this(sourceText, translations, sourceLabel, movieTitle, movieUrl, null);
    }
    public SourceExample {
        if (sourceText == null || sourceText.isBlank()) throw new IllegalArgumentException("Example text must be nonblank");
        translations = translations == null ? List.of() : List.copyOf(translations);
    }
}
