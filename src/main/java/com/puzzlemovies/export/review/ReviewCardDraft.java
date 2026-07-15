package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;

public record ReviewCardDraft(String originalText,
                              String instanceText,
                              String translationText,
                              String sourceContext,
                              ExportRecord.RecordKind recordKind) {
    public ReviewCardDraft {
        if (originalText == null || originalText.isBlank()) {
            throw new IllegalArgumentException("originalText must be non-blank");
        }
        if (recordKind == null) {
            throw new IllegalArgumentException("recordKind is required");
        }
        originalText = originalText.trim();
        instanceText = normalizeOptional(instanceText);
        translationText = translationText == null ? "" : translationText.trim();
        sourceContext = normalizeOptional(sourceContext);
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
