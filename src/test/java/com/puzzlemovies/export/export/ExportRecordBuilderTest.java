package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportRecordBuilderTest {
    private final ExportRecordBuilder builder = new ExportRecordBuilder();

    @Test
    void rendersTwoExamplesWithMovieContext() {
        DictionaryWord word = new DictionaryWord("Run", Set.of("бежать"), "run", false);
        List<PhraseExample> examples = List.of(
                new PhraseExample("run", "I am running.", Set.of("Я бегу."), "Arrival", "https://puzzle-movies.com/movie/1"),
                new PhraseExample("run", "Run home.", Set.of("Беги домой."), null, null));

        List<ExportRecord> records = builder.buildWordRecords(List.of(word), Map.of("run", examples));

        assertEquals(1, records.size());
        assertTrue(records.get(0).back().contains("бежать"));
        assertTrue(records.get(0).back().contains("I am running."));
        assertTrue(records.get(0).back().contains("<a href=\"https://puzzle-movies.com/movie/1\">Arrival</a>"));
    }

    @Test
    void preservesBlankTranslationAndUnmatchedItems() {
        DictionaryWord word = new DictionaryWord("Silence", Set.of(), "silence", false);
        DictionaryPhrase phrase = new DictionaryPhrase("No translation", Set.of(), null, null, Set.of("no", "translation"), "no translation", false);

        List<ExportRecord> records = builder.buildRecords(List.of(word), List.of(phrase), Map.of(), true, true);

        assertEquals(2, records.size());
        assertEquals("", records.get(0).back());
        assertEquals("", records.get(1).back());
    }

    @Test
    void mergesMultipleTranslationsIntoBackContent() {
        DictionaryWord word = new DictionaryWord("Run", Set.of("бежать", "запускать"), "run", false);

        ExportRecord record = builder.buildWordRecords(List.of(word), Map.of()).get(0);

        assertTrue(record.back().contains("бежать"));
        assertTrue(record.back().contains("запускать"));
    }

    @Test
    void representativeSizeBuildAndFormatCompletesWithinTenMinuteCriterion() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            List<DictionaryWord> words = new ArrayList<>();
            List<DictionaryPhrase> phrases = new ArrayList<>();
            for (int index = 0; index < 500; index++) {
                words.add(new DictionaryWord("word-" + index, Set.of("translation-" + index), "word-" + index, false));
                phrases.add(new DictionaryPhrase(
                        "phrase " + index,
                        Set.of("phrase translation " + index),
                        null,
                        null,
                        Set.of("phrase", String.valueOf(index)),
                        "phrase " + index,
                        false));
            }

            List<ExportRecord> records = builder.buildRecords(words, phrases, Map.of(), true, true);
            String output = new AnkiExportFormatter().formatTsv(records);

            assertEquals(1000, records.size());
            assertEquals(1000, output.lines().count());
        });
    }
}
