package util;

import model.DictionaryPhrase;
import model.DictionaryWord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Deduplicator {
    private final TextNormalizer normalizer;

    public Deduplicator(TextNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public List<DictionaryWord> dedupeWords(List<DictionaryWord> words) {
        Map<String, DictionaryWord> merged = new LinkedHashMap<>();
        for (DictionaryWord word : words) {
            String key = word.normalizedKey();
            if (key == null || key.isBlank()) {
                key = normalizer.normalizeWord(word.english());
            }
            if (key.isBlank()) {
                continue;
            }
            DictionaryWord existing = merged.get(key);
            if (existing == null) {
                merged.put(key, word);
            } else if ((existing.russian() == null || existing.russian().isBlank())
                    && word.russian() != null && !word.russian().isBlank()) {
                merged.put(key, new DictionaryWord(existing.english(), word.russian(), existing.normalizedKey()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    public List<DictionaryPhrase> dedupePhrases(List<DictionaryPhrase> phrases) {
        Map<String, DictionaryPhrase> merged = new LinkedHashMap<>();
        for (DictionaryPhrase phrase : phrases) {
            String key = normalizer.normalizeText(phrase.english());
            if (key.isBlank()) {
                continue;
            }
            DictionaryPhrase existing = merged.get(key);
            if (existing == null) {
                merged.put(key, phrase);
            } else if ((existing.russian() == null || existing.russian().isBlank())
                    && phrase.russian() != null && !phrase.russian().isBlank()) {
                merged.put(key, new DictionaryPhrase(
                        existing.english(),
                        phrase.russian(),
                        existing.movieTitle(),
                        existing.movieUrl(),
                        existing.normalizedTokens()
                ));
            }
        }
        return new ArrayList<>(merged.values());
    }
}
