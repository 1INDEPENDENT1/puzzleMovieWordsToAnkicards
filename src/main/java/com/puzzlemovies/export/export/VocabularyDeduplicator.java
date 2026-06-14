package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class VocabularyDeduplicator {
    public List<DictionaryWord> dedupeWords(List<DictionaryWord> words) {
        Map<String, DictionaryWord> merged = new LinkedHashMap<>();
        for (DictionaryWord word : words) {
            merged.merge(word.matchingIdentity(), word, this::mergeWords);
        }
        return new ArrayList<>(merged.values());
    }

    public List<DictionaryPhrase> dedupePhrases(List<DictionaryPhrase> phrases) {
        Map<String, DictionaryPhrase> merged = new LinkedHashMap<>();
        for (DictionaryPhrase phrase : phrases) {
            merged.merge(phrase.dedupeIdentity(), phrase, this::mergePhrases);
        }
        return new ArrayList<>(merged.values());
    }

    private DictionaryWord mergeWords(DictionaryWord first, DictionaryWord next) {
        return new DictionaryWord(
                first.sourceText(),
                mergeTranslations(first.translations(), next.translations()),
                first.matchingIdentity(),
                first.normalizationFallbackUsed() || next.normalizationFallbackUsed());
    }

    private DictionaryPhrase mergePhrases(DictionaryPhrase first, DictionaryPhrase next) {
        return new DictionaryPhrase(
                first.sourceText(),
                mergeTranslations(first.translations(), next.translations()),
                first.movieTitle() != null ? first.movieTitle() : next.movieTitle(),
                first.movieUrl() != null ? first.movieUrl() : next.movieUrl(),
                mergeTokens(first.matchingTokens(), next.matchingTokens()),
                first.dedupeIdentity(),
                first.normalizationFallbackUsed() || next.normalizationFallbackUsed());
    }

    private Set<String> mergeTranslations(Set<String> first, Set<String> next) {
        return mergeTokens(first, next);
    }

    private Set<String> mergeTokens(Set<String> first, Set<String> next) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(first);
        merged.addAll(next);
        return merged;
    }
}
