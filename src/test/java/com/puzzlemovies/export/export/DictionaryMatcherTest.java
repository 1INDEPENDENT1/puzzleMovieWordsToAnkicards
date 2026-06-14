package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DictionaryMatcherTest {
    @Test
    void matchesExactAndInflectedTokensWithTwoExampleCap() {
        DictionaryWord run = new DictionaryWord("run", Set.of("бежать"), "run", false);
        List<DictionaryPhrase> phrases = List.of(
                phrase("I run", "run"),
                phrase("I am running", "run"),
                phrase("Runs fast", "run"),
                phrase("Moon shines", "moon"));

        Map<String, List<PhraseExample>> matches = new DictionaryMatcher().match(List.of(run), phrases);

        assertEquals(2, matches.get("run").size());
        assertEquals("I run", matches.get("run").get(0).phraseSourceText());
        assertEquals("I am running", matches.get("run").get(1).phraseSourceText());
    }

    @Test
    void omitsNoMatchWords() {
        DictionaryWord word = new DictionaryWord("ocean", Set.of(), "ocean", false);

        Map<String, List<PhraseExample>> matches = new DictionaryMatcher().match(List.of(word), List.of(phrase("Moon", "moon")));

        assertFalse(matches.containsKey("ocean"));
    }

    private DictionaryPhrase phrase(String source, String token) {
        return new DictionaryPhrase(source, Set.of("translation"), null, null, Set.of(token), source.toLowerCase(), false);
    }
}
