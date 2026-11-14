package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;
import org.drugov.lingua.morph.LuceneLemmatizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VocabularyNormalizerTest {
    @Test
    void usesLemmaIdentityWhenAvailable() {
        VocabularyNormalizer normalizer = new VocabularyNormalizer(new StubLemmatizer());

        VocabularyNormalizer.NormalizedText normalized = normalizer.normalizeSingle("Running!");

        assertEquals("run", normalized.identity());
        assertTrue(normalized.tokens().contains("run"));
    }

    @Test
    void usesFullMatchingFormWithOnlyOneLeadingPrefixRemoved() {
        VocabularyNormalizer normalizer = new VocabularyNormalizer(ExportTestFixtures.testLemmatizer());

        assertEquals("run up the hill", normalizer.normalizeSingle("to running up the hill").identity());
        assertEquals("doofus", normalizer.normalizeSingle("a doofus").identity());
        assertEquals("actor", normalizer.normalizeSingle("an actor").identity());
        assertEquals("home", normalizer.normalizeSingle("the home").identity());
        assertEquals("to", normalizer.normalizeSingle("to").identity());
        assertEquals("a", normalizer.normalizeSingle("a").identity());
        assertEquals("go go", normalizer.normalizeSingle("go go").identity());
    }

    @Test
    void realLemmatizerKeepsNumbersContractionsAndRepeatedPositions() {
        var normalizer = new VocabularyNormalizer(new LuceneLemmatizer());

        var normalized = normalizer.normalizePhrase("Went, went 3.5 miles? I don't.");

        assertEquals(List.of(List.of("go", "go", "3.5", "mile"), List.of("i", "don't")),
                normalized.sentences());
        assertTrue(normalized.fallbackUsed());
    }

    @Test
    void fallsBackToCleanedTokenWhenLemmaFails() {
        VocabularyNormalizer normalizer = new VocabularyNormalizer(new ThrowingLemmatizer());

        VocabularyNormalizer.NormalizedText normalized = normalizer.normalizeSingle("Moon!");

        assertEquals("moon", normalized.identity());
        assertTrue(normalized.fallbackUsed());
    }

    private static class StubLemmatizer implements Lemmatizer {
        @Override
        public String lemma(String text) {
            return "running".equals(text) ? "run" : text;
        }

        @Override
        public Set<String> lemmas(String text) {
            return Set.of(lemma(text));
        }

        @Override
        public org.drugov.lingua.model.LemmaResult analyze(String text) {
            return null;
        }

        @Override
        public List<String> tokenize(String text) {
            return List.of(text);
        }
    }

    private static final class ThrowingLemmatizer extends StubLemmatizer {
        @Override
        public String lemma(String text) {
            throw new IllegalStateException("boom");
        }
    }
}
