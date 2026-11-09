package com.puzzlemovies.export.export;
import java.util.Objects;
public record ParseIssue(SourcePosition position, Reason reason) {
    public enum Reason { MISSING_ORIGINAL, UNUSABLE_IDENTITY }
    public ParseIssue { Objects.requireNonNull(position); Objects.requireNonNull(reason); }
}
