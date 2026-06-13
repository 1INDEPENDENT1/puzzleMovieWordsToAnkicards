package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;
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
