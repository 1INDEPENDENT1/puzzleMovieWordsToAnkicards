package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.puzzlemovies.export.export.VocabularyNormalizer.NormalizedText;

@Component
public class DictionaryMatcher {
    private static final int EXAMPLE_CAP = 2;
    private static final int MAX_TOKEN_DISTANCE = 4;

    public Map<String, List<PhraseExample>> match(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        CompleteWordExampleMatches completeMatches = matchAll(words, phrases);
        Map<String, List<PhraseExample>> cappedMatches = new LinkedHashMap<>();
        completeMatches.examplesByWord().forEach((wordIdentity, examples) ->
                cappedMatches.put(wordIdentity, examples.subList(0, Math.min(EXAMPLE_CAP, examples.size()))));
        return cappedMatches;
    }

    public CompleteWordExampleMatches matchAll(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        if (words == null || phrases == null) {
            return CompleteWordExampleMatches.empty();
        }
        List<NormalizedText> wordForms = words.stream().map(word -> {
            List<String> tokens = List.of(word.matchingIdentity().split("\\s+"));
            return new NormalizedText(word.matchingIdentity(), Set.copyOf(tokens),
                    word.normalizationFallbackUsed(), List.of(tokens));
        }).toList();
        List<NormalizedText> phraseForms = phrases.stream().map(phrase -> new NormalizedText(
                phrase.dedupeIdentity(), phrase.matchingTokens(), phrase.normalizationFallbackUsed(),
                phrase.matchingSentences())).toList();
        List<List<Integer>> matches = matchingExampleIndexes(wordForms, phraseForms);
        Map<String, List<PhraseExample>> examplesByWord = new LinkedHashMap<>();
        for (int wordIndex = 0; wordIndex < words.size(); wordIndex++) {
            String wordIdentity = words.get(wordIndex).sourceIdentity();
            List<PhraseExample> examples = new ArrayList<>();
            for (int phraseIndex : matches.get(wordIndex)) {
                DictionaryPhrase phrase = phrases.get(phraseIndex);
                examples.add(new PhraseExample(wordIdentity, phrase.sourceText(), phrase.translations(),
                        phrase.movieTitle(), phrase.movieUrl()));
            }
            examplesByWord.put(wordIdentity, examples);
        }
        return new CompleteWordExampleMatches(examplesByWord);
    }

    /** Each result row contains the zero-based indexes of the examples matching that word, in input order. */
    public List<List<Integer>> matchingExampleIndexes(List<NormalizedText> words, List<NormalizedText> examples) {
        Map<String, List<Integer>> candidatesByToken = new LinkedHashMap<>();
        for (int exampleIndex = 0; exampleIndex < examples.size(); exampleIndex++) {
            for (String token : examples.get(exampleIndex).tokens()) {
                candidatesByToken.computeIfAbsent(token, ignored -> new ArrayList<>()).add(exampleIndex);
            }
        }

        List<List<Integer>> matches = new ArrayList<>();
        for (NormalizedText word : words) {
            List<Integer> matchingIndexes = new ArrayList<>();
            if (!word.sentences().isEmpty()) {
                List<String> parts = word.sentences().getFirst();
                for (int candidate : candidatesByToken.getOrDefault(parts.getFirst(), List.of())) {
                    if (matches(word, examples.get(candidate))) {
                        matchingIndexes.add(candidate);
                    }
                }
            }
            matches.add(List.copyOf(matchingIndexes));
        }
        return List.copyOf(matches);
    }

    public boolean matches(NormalizedText word, NormalizedText example) {
        if (word.sentences().isEmpty() || !example.tokens().containsAll(word.tokens())) {
            return false;
        }
        List<String> parts = word.sentences().getFirst();
        if (parts.size() == 1) {
            return true;
        }
        return example.sentences().stream().anyMatch(sentence -> containsExpression(parts, sentence));
    }

    private boolean containsExpression(List<String> parts, List<String> sentence) {
        List<Integer> reachable = new ArrayList<>();
        for (int index = 0; index < sentence.size(); index++) {
            if (sentence.get(index).equals(parts.getFirst())) {
                reachable.add(index);
            }
        }
        for (int partIndex = 1; partIndex < parts.size() && !reachable.isEmpty(); partIndex++) {
            List<Integer> next = new ArrayList<>();
            String part = parts.get(partIndex);
            int lastAdded = -1;
            for (int previous : reachable) {
                int firstCandidate = Math.max(previous + 1, lastAdded + 1);
                int lastCandidate = Math.min(previous + MAX_TOKEN_DISTANCE, sentence.size() - 1);
                for (int candidate = firstCandidate; candidate <= lastCandidate; candidate++) {
                    if (sentence.get(candidate).equals(part)) {
                        next.add(candidate);
                        lastAdded = candidate;
                    }
                }
            }
            reachable = next;
        }
        return !reachable.isEmpty();
    }
}
