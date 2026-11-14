package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class VocabularyDeduplicator {
    public List<DictionaryWord> dedupeWords(List<DictionaryWord> words) {
        Map<String, DictionaryWord> merged = new LinkedHashMap<>();
        for (DictionaryWord word : words) {
            merged.merge(word.sourceIdentity(), word, this::mergeWords);
        }
        return new ArrayList<>(merged.values());
    }

    public List<DictionaryPhrase> dedupePhrases(List<DictionaryPhrase> phrases) {
        Map<PhraseSourceKey, DictionaryPhrase> merged = new LinkedHashMap<>();
        for (DictionaryPhrase phrase : phrases) {
            merged.merge(sourceKey(phrase), phrase, this::mergePhrases);
        }
        return new ArrayList<>(merged.values());
    }

    private DictionaryWord mergeWords(DictionaryWord first, DictionaryWord next) {
        return new DictionaryWord(
                first.sourceText(),
                mergeTranslations(first.translations(), next.translations()),
                first.matchingIdentity(),
                first.normalizationFallbackUsed() || next.normalizationFallbackUsed(),
                Stream.concat(first.sources().stream(), next.sources().stream()).toList());
    }

    private DictionaryPhrase mergePhrases(DictionaryPhrase first, DictionaryPhrase next) {
        return new DictionaryPhrase(
                first.sourceText(),
                mergeTranslations(first.translations(), next.translations()),
                first.movieTitle() != null ? first.movieTitle() : next.movieTitle(),
                first.movieUrl() != null ? first.movieUrl() : next.movieUrl(),
                first.matchingTokens(),
                first.dedupeIdentity(),
                first.normalizationFallbackUsed() || next.normalizationFallbackUsed(),
                first.matchingSentences(),
                Stream.concat(first.sources().stream(), next.sources().stream()).toList());
    }

    private Set<String> mergeTranslations(Set<String> first, Set<String> next) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(first);
        merged.addAll(next);
        return merged;
    }

    private PhraseSourceKey sourceKey(DictionaryPhrase phrase) {
        var source = phrase.sources().isEmpty() ? null : phrase.sources().getFirst();
        String stableId = source == null ? null : source.position().sourceItemId();
        String label = source == null ? null : source.sourceLabel();
        List<String> translations = source == null ? List.copyOf(phrase.translations()) : source.translations();
        String original = phrase.sourceText().replaceAll("(?U)\\s+", " ").strip();
        return new PhraseSourceKey(stableId, stableId == null ? original : null,
                stableId == null ? translations : List.of(), label, phrase.movieTitle(), phrase.movieUrl());
    }

    private record PhraseSourceKey(String stableId, String original, List<String> translations, String label,
                                   String movieTitle, String movieUrl) {}
}
