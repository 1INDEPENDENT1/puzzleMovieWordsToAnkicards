package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DictionaryMatcher {
    private static final int EXAMPLE_CAP = 2;

    public Map<String, List<PhraseExample>> match(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        Map<String, List<DictionaryPhrase>> phraseIndex = indexPhrases(phrases);
        Map<String, List<PhraseExample>> examplesByWord = new LinkedHashMap<>();

        for (DictionaryWord word : words) {
            List<DictionaryPhrase> candidates = phraseIndex.getOrDefault(word.matchingIdentity(), List.of());
            List<PhraseExample> examples = new ArrayList<>();
            for (DictionaryPhrase phrase : candidates) {
                examples.add(new PhraseExample(
                        word.matchingIdentity(),
                        phrase.sourceText(),
                        phrase.translations(),
                        phrase.movieTitle(),
                        phrase.movieUrl()));
                if (examples.size() == EXAMPLE_CAP) {
                    break;
                }
            }
            if (!examples.isEmpty()) {
                examplesByWord.put(word.matchingIdentity(), examples);
            }
        }

        return examplesByWord;
    }

    private Map<String, List<DictionaryPhrase>> indexPhrases(List<DictionaryPhrase> phrases) {
        Map<String, List<DictionaryPhrase>> index = new LinkedHashMap<>();
        for (DictionaryPhrase phrase : phrases) {
            for (String token : phrase.matchingTokens()) {
                index.computeIfAbsent(token, ignored -> new ArrayList<>()).add(phrase);
            }
        }
        return index;
    }
}
