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
        CompleteWordExampleMatches completeMatches = matchAll(words, phrases);
        Map<String, List<PhraseExample>> cappedMatches = new LinkedHashMap<>();
        completeMatches.examplesByWord().forEach((wordIdentity, examples) ->
                cappedMatches.put(wordIdentity, examples.subList(0, Math.min(EXAMPLE_CAP, examples.size()))));
        return cappedMatches;
    }

    public CompleteWordExampleMatches matchAll(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        Map<String, List<DictionaryPhrase>> phraseIndex = indexPhrases(phrases);
        Map<String, List<PhraseExample>> examplesByWord = new LinkedHashMap<>();

        for (DictionaryWord word : safeWords(words)) {
            List<DictionaryPhrase> candidates = phraseIndex.getOrDefault(word.matchingIdentity(), List.of());
            List<PhraseExample> examples = new ArrayList<>();
            for (DictionaryPhrase phrase : candidates) {
                examples.add(new PhraseExample(
                        word.matchingIdentity(),
                        phrase.sourceText(),
                        phrase.translations(),
                        phrase.movieTitle(),
                        phrase.movieUrl()));
            }
            if (!examples.isEmpty()) {
                examplesByWord.put(word.matchingIdentity(), examples);
            }
        }

        return new CompleteWordExampleMatches(examplesByWord);
    }

    private Map<String, List<DictionaryPhrase>> indexPhrases(List<DictionaryPhrase> phrases) {
        Map<String, List<DictionaryPhrase>> index = new LinkedHashMap<>();
        for (DictionaryPhrase phrase : safePhrases(phrases)) {
            for (String token : phrase.matchingTokens()) {
                index.computeIfAbsent(token, ignored -> new ArrayList<>()).add(phrase);
            }
        }
        return index;
    }

    private List<DictionaryWord> safeWords(List<DictionaryWord> words) {
        return words == null ? List.of() : words;
    }

    private List<DictionaryPhrase> safePhrases(List<DictionaryPhrase> phrases) {
        return phrases == null ? List.of() : phrases;
    }
}
