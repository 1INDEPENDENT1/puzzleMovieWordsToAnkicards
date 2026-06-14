package com.puzzlemovies.export.model;

public enum ExportPhase {
    PENDING,
    FETCHING_WORDS,
    FETCHING_PHRASES,
    PARSING,
    DEDUPLICATING,
    MATCHING,
    WRITING,
    COMPLETED,
    FAILED
}
