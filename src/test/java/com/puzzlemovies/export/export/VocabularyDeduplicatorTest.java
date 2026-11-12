package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VocabularyDeduplicatorTest {
    private final VocabularyDeduplicator deduplicator = new VocabularyDeduplicator();

    @Test
    void mergesDuplicateWordsAndConflictingTranslations() {
        List<DictionaryWord> words = List.of(
                new DictionaryWord("Run", Set.of("бежать"), "run", false),
                new DictionaryWord("run", Set.of("запускать"), "run", false),
                new DictionaryWord("running", Set.of("прибегать"), "run", false),
                new DictionaryWord("to mince", Set.of(), "to", false),
                new DictionaryWord("to run", Set.of(), "to", false));

        List<DictionaryWord> result = deduplicator.dedupeWords(words);

        assertEquals(List.of("Run", "running", "to mince", "to run"),
                result.stream().map(DictionaryWord::sourceText).toList());
        assertEquals("Run", result.get(0).sourceText());
        assertTrue(result.get(0).translations().contains("бежать"));
        assertTrue(result.get(0).translations().contains("запускать"));
    }

    @Test
    void keepsSamePhraseFromEachFilmAndMergesIdenticalSourceOccurrences() {
        List<DictionaryPhrase> phrases = List.of(
                new DictionaryPhrase("I run", Set.of(), "Movie", "https://puzzle-movies.com/movie/1", Set.of("i", "run"), "i run", false),
                new DictionaryPhrase("I run", Set.of("Я бегу"), "Another movie", "https://puzzle-movies.com/movie/2", Set.of("i", "run"), "i run", false),
                new DictionaryPhrase("I run", Set.of(), "Movie", "https://puzzle-movies.com/movie/1", Set.of("i", "run"), "i run", false));

        List<DictionaryPhrase> result = deduplicator.dedupePhrases(phrases);

        assertEquals(2, result.size());
        assertEquals("Movie", result.get(0).movieTitle());
        assertEquals(Set.of(), result.get(0).translations());
        assertEquals(Set.of("Я бегу"), result.get(1).translations());
    }
}
