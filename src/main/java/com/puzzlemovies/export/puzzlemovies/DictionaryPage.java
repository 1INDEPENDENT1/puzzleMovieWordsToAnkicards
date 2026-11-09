package com.puzzlemovies.export.puzzlemovies;
import java.net.URI;
import java.util.Objects;
public record DictionaryPage(DictionarySection section, int pageNumber, URI sourceUri, String html, PageInspection inspection) {
    public DictionaryPage {
        Objects.requireNonNull(section); Objects.requireNonNull(sourceUri);
        Objects.requireNonNull(html); Objects.requireNonNull(inspection);
        if (pageNumber < 1) throw new IllegalArgumentException("Page number must be positive");
    }
    public record PageInspection(DictionarySection section, int declaredTotal, int currentPage,
                                 Integer nextPage, int sourceRecordCount, boolean recognized, boolean authenticated) {
        public PageInspection {
            Objects.requireNonNull(section);
            if (declaredTotal < 0 || currentPage < 1 || sourceRecordCount < 0 || (nextPage != null && nextPage < 1))
                throw new IllegalArgumentException("Invalid dictionary page counts");
        }
    }
}
