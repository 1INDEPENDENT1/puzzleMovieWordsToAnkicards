package com.puzzlemovies.export.puzzlemovies;

public class PuzzleMoviesSessionExpiredException extends RuntimeException {
    public PuzzleMoviesSessionExpiredException() {
        super("Puzzle-Movies session expired or returned an anonymous page. Please sign in again.");
    }
}
