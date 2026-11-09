package com.puzzlemovies.export.puzzlemovies;

import java.util.Objects;

/** Stable source counter versus rows actually returned by a fully traversed section. */
public record SourceCountDiscrepancy(DictionarySection section, int declaredTotal, int retrievedTotal) {
    public SourceCountDiscrepancy {
        Objects.requireNonNull(section);
        if (declaredTotal < 0 || retrievedTotal < 0 || declaredTotal == retrievedTotal)
            throw new IllegalArgumentException("Invalid source counter discrepancy");
    }
}
