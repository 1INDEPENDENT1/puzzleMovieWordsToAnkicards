package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryParserTest {
    private final DictionaryParser parser = new DictionaryParser(
            new VocabularyNormalizer(ExportTestFixtures.testLemmatizer()));

    @Test
    void parsesWordsFromTableAndCardRows() {
        List<DictionaryWord> words = parser.parseWords(ExportTestFixtures.wordPages());

        assertEquals(3, words.size());
        assertEquals("Run", words.get(0).sourceText());
        assertEquals("run", words.get(0).matchingIdentity());
        assertTrue(words.get(0).translations().contains("бежать"));
        assertEquals("empty translation", words.get(1).sourceText());
        assertTrue(words.get(1).translations().isEmpty());
    }

    @Test
    void parsesPhrasesWithMovieTitleAndUrl() {
        List<DictionaryPhrase> phrases = parser.parsePhrases(ExportTestFixtures.phrasePages());

        assertEquals(2, phrases.size());
        assertEquals("I am running home.", phrases.get(0).sourceText());
        assertTrue(phrases.get(0).matchingTokens().contains("run"));
        assertEquals("Arrival", phrases.get(0).movieTitle());
        assertEquals("https://puzzle-movies.com/movie/1", phrases.get(0).movieUrl());
    }
}
