package com.puzzlemovies.export.export;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import java.util.Objects;
public record SourcePosition(DictionarySection section, int pageNumber, int rowNumber, String sourceItemId) {
    public SourcePosition {
        Objects.requireNonNull(section);
        if (pageNumber < 1 || rowNumber < 1) throw new IllegalArgumentException("Source position must be positive");
    }
}
