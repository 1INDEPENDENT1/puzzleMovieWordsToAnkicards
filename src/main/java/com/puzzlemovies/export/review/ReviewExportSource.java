package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.CompleteWordExampleMatches;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;

import java.util.List;

/**
 * App-owned structured export data retained for the current process so review cards never need to
 * be reconstructed from TSV or rendered export markup.
 */
public record ReviewExportSource(List<DictionaryWord> words,
                                 List<DictionaryPhrase> examples,
                                 CompleteWordExampleMatches completeMatches) {
    public ReviewExportSource {
        words = words == null ? List.of() : List.copyOf(words);
        examples = examples == null ? List.of() : List.copyOf(examples);
        completeMatches = completeMatches == null ? CompleteWordExampleMatches.empty() : completeMatches;
    }
}
