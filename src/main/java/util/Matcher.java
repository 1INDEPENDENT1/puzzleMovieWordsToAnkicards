package util;

import model.DictionaryPhrase;
import model.DictionaryWord;
import model.PhraseExample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Matcher {
    private static final int EXAMPLE_CAP = 2;

    private final TextNormalizer normalizer;

    public Matcher(TextNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public Map<String, List<PhraseExample>> match(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        WordIndex index = new WordIndex(phrases, normalizer);
        Map<String, List<PhraseExample>> matches = new HashMap<>();

        for (DictionaryWord word : words) {
            String wordKey = word.normalizedKey();
            if (wordKey == null || wordKey.isBlank()) {
                wordKey = normalizer.normalizeWord(word.english());
            }
            if (wordKey.isBlank()) {
                continue;
            }
            List<DictionaryPhrase> candidates = index.findMatches(wordKey);
            List<PhraseExample> examples = new ArrayList<>();
            for (DictionaryPhrase phrase : candidates) {
                examples.add(new PhraseExample(
                        wordKey,
                        phrase.english(),
                        phrase.russian(),
                        phrase.movieTitle(),
                        phrase.movieUrl()
                ));
                if (examples.size() >= EXAMPLE_CAP) {
                    break;
                }
            }
            if (!examples.isEmpty()) {
                matches.put(wordKey, examples);
            }
        }

        return matches;
    }
}
