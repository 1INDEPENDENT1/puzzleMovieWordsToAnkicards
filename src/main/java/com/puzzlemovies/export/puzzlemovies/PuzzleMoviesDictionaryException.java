package com.puzzlemovies.export.puzzlemovies;

/** Source failure containing only trusted category and numeric request context. */
public class PuzzleMoviesDictionaryException extends RuntimeException {
    public enum Category { AUTHENTICATION, HTTP, NETWORK, INTERRUPTED, STRUCTURE, PAGINATION, SOURCE_CHANGED }
    private final Category category;
    private final DictionarySection section;
    private final int pageNumber;
    private final Integer statusCode;

    public PuzzleMoviesDictionaryException(Category category, DictionarySection section, int pageNumber, Integer statusCode) {
        super("Dictionary " + category + " failure (section=" + section + ", page=" + pageNumber
                + (statusCode == null ? "" : ", HTTP=" + statusCode) + "). Please retry or sign in again.");
        this.category = category;
        this.section = section;
        this.pageNumber = pageNumber;
        this.statusCode = statusCode;
    }
    public Category category() { return category; }
    public DictionarySection section() { return section; }
    public int pageNumber() { return pageNumber; }
    public Integer statusCode() { return statusCode; }
}
