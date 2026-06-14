package com.puzzlemovies.export.export;

public record ExportRecord(String front, String back, RecordKind recordKind) {
    public enum RecordKind {
        WORD,
        PHRASE
    }

    public ExportRecord {
        if (front == null || front.isBlank()) {
            throw new IllegalArgumentException("front must be non-blank");
        }
        front = front.trim();
        back = back == null ? "" : back;
        if (recordKind == null) {
            throw new IllegalArgumentException("recordKind is required");
        }
    }
}
