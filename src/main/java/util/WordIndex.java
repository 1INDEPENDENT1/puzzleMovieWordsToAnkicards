package util;

import model.DictionaryPhrase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordIndex {
    private final Map<String, List<DictionaryPhrase>> index = new HashMap<>();

    public WordIndex(List<DictionaryPhrase> phrases, TextNormalizer normalizer) {
        for (DictionaryPhrase phrase : phrases) {
            List<String> tokens = phrase.normalizedTokens();
            if (tokens == null || tokens.isEmpty()) {
                tokens = normalizer.normalizeTokens(phrase.english());
            }
            for (String token : tokens) {
                String key = normalizer.normalizeWord(token);
                if (key.isBlank()) {
                    continue;
                }
                index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(phrase);
            }
        }
    }

    public List<DictionaryPhrase> findMatches(String normalizedWord) {
        if (normalizedWord == null || normalizedWord.isBlank()) {
            return List.of();
        }
        return index.getOrDefault(normalizedWord, List.of());
    }
}
