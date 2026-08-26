package com.puzzlemovies.export.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete, source-ordered matches used by review-card creation. Export rendering can apply its
 * own display cap without losing the remaining associations.
 */
public record CompleteWordExampleMatches(Map<String, List<PhraseExample>> examplesByWord) {
    public CompleteWordExampleMatches {
        LinkedHashMap<String, List<PhraseExample>> copy = new LinkedHashMap<>();
        if (examplesByWord != null) {
            examplesByWord.forEach((wordIdentity, examples) -> {
                if (wordIdentity == null || wordIdentity.isBlank() || examples == null || examples.isEmpty()) {
                    return;
                }
                copy.put(wordIdentity.trim(), List.copyOf(new ArrayList<>(examples)));
            });
        }
        examplesByWord = Collections.unmodifiableMap(copy);
    }

    public static CompleteWordExampleMatches empty() {
        return new CompleteWordExampleMatches(Map.of());
    }

    public List<PhraseExample> forWord(String wordIdentity) {
        if (wordIdentity == null) {
            return List.of();
        }
        return examplesByWord.getOrDefault(wordIdentity.trim(), List.of());
    }
}
