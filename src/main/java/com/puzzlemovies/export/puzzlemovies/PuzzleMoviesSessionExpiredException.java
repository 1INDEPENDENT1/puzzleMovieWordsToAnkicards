package com.puzzlemovies.export.puzzlemovies;

public class PuzzleMoviesSessionExpiredException extends PuzzleMoviesDictionaryException {
    public PuzzleMoviesSessionExpiredException() { this(DictionarySection.WORD, 1); }
    public PuzzleMoviesSessionExpiredException(DictionarySection section, int pageNumber) {
        super(Category.AUTHENTICATION, section, pageNumber, null);
    }
}
