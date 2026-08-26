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
    void retainsAllMatchesInSourceOrderForReviewWhileKeepingExportCap() {
        DictionaryWord run = new DictionaryWord("run", Set.of("бежать"), "run", false);
        List<DictionaryPhrase> phrases = List.of(
                phrase("I run", "run"),
                phrase("I am running", "run"),
                phrase("Runs fast", "run"));

        DictionaryMatcher matcher = new DictionaryMatcher();
        CompleteWordExampleMatches allMatches = matcher.matchAll(List.of(run), phrases);

        assertEquals(3, allMatches.forWord("run").size());
        assertEquals("I run", allMatches.forWord("run").get(0).phraseSourceText());
        assertEquals("Runs fast", allMatches.forWord("run").get(2).phraseSourceText());
        assertEquals(2, matcher.match(List.of(run), phrases).get("run").size());
    }

    @Test
    void associatesOneExampleWithEveryMatchingWordWithoutUnrelatedMatches() {
        DictionaryWord run = new DictionaryWord("run", Set.of(), "run", false);
        DictionaryWord home = new DictionaryWord("home", Set.of(), "home", false);
        DictionaryWord moon = new DictionaryWord("moon", Set.of(), "moon", false);
        DictionaryPhrase phrase = new DictionaryPhrase(
                "I run home",
                Set.of("translation"),
                null,
                null,
                Set.of("run", "home"),
                "i run home",
                false);

        CompleteWordExampleMatches matches = new DictionaryMatcher().matchAll(List.of(run, home, moon), List.of(phrase));

        assertEquals(1, matches.forWord("run").size());
        assertEquals(1, matches.forWord("home").size());
        assertFalse(matches.examplesByWord().containsKey("moon"));
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
