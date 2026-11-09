package com.puzzlemovies.export.export;
import java.util.List;
import java.util.Objects;
public record RawDictionaryEntry(SourcePosition position, String sourceText, List<String> translations,
                                 List<SourceExample> examples, String sourceLabel, String movieTitle, String movieUrl) {
    public RawDictionaryEntry {
        Objects.requireNonNull(position);
        translations = translations == null ? List.of() : List.copyOf(translations);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }
}
