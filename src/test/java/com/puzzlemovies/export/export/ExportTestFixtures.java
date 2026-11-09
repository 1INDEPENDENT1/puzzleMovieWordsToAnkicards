package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ExportTestFixtures {
    public static String wordRow(String original, String translation) {
        return "<tr class='dict__video__list-table__row'><td><div class='translate_line_without_popup'>" + original
                + "</div><div class='dict__video__list-table__word__translate'>" + translation
                + "</div></td><td class='dict__video__list-table__phrase'></td><td></td></tr>";
    }
    public static String phraseRow(String original, String translation, String movie) {
        return "<tr class='dict__video__list-table__row'><td>" + movie
                + "</td><td><div class='dict__video__list-table__phrase__eng'>" + original
                + "</div><div class='dict__video__list-table__phrase__rus'>" + translation + "</div></td><td></td></tr>";
    }
    public static final String WORD_PAGE = "<table class='dict__video__list-table'>"
            + wordRow("Run", "бежать") + wordRow("empty translation", "") + wordRow("Moon", "луна") + "</table>";
    public static final String PHRASE_PAGE = "<table class='dict__video__list-table'>"
            + phraseRow("I am running home.", "Я бегу домой.", "<a class='dict__video__list-table__info__balloon__title' href='/movie/1'>Arrival</a>")
            + phraseRow("The moon is bright.", "Луна яркая.", "<a class='dict__video__list-table__info__balloon__title' href='/movie/2'>Moon</a>") + "</table>";

    public static final DictionaryWord RUN_WORD = new DictionaryWord("Run", Set.of("бежать"), "run", false);
    public static final DictionaryPhrase RUNNING_PHRASE = new DictionaryPhrase(
            "I am running home.",
            Set.of("Я бегу домой."),
            "Arrival",
            "https://puzzle-movies.com/movie/1",
            Set.of("i", "am", "run", "home"),
            "i am run home",
            false);

    private ExportTestFixtures() {
    }

    public static com.puzzlemovies.export.puzzlemovies.DictionaryPage page(
            com.puzzlemovies.export.puzzlemovies.DictionarySection section, String html, int count) {
        return new com.puzzlemovies.export.puzzlemovies.DictionaryPage(section, 1,
                java.net.URI.create("https://puzzle-movies.com/dictionary?item=" + section.item()), html,
                new com.puzzlemovies.export.puzzlemovies.DictionaryPage.PageInspection(section, count, 1, null, count, true, true));
    }
    public static List<com.puzzlemovies.export.puzzlemovies.DictionaryPage> wordPages() {
        return List.of(page(com.puzzlemovies.export.puzzlemovies.DictionarySection.WORD, WORD_PAGE, 3));
    }
    public static List<com.puzzlemovies.export.puzzlemovies.DictionaryPage> phrasePages() {
        return List.of(page(com.puzzlemovies.export.puzzlemovies.DictionarySection.PHRASE, PHRASE_PAGE, 2));
    }

    public static DictionaryWord word(String sourceText, String translation, String matchingIdentity) {
        return new DictionaryWord(sourceText,
                translation == null || translation.isBlank() ? Set.of() : Set.of(translation),
                matchingIdentity,
                false);
    }

    public static DictionaryPhrase phrase(String sourceText, String translation, String... matchingTokens) {
        return new DictionaryPhrase(sourceText,
                translation == null || translation.isBlank() ? Set.of() : Set.of(translation),
                null,
                null,
                Set.of(matchingTokens),
                sourceText.toLowerCase(Locale.ROOT),
                false);
    }

    public static Lemmatizer testLemmatizer() {
        return new Lemmatizer() {
            @Override
            public String lemma(String text) {
                return switch (text.toLowerCase(Locale.ROOT)) {
                    case "running" -> "run";
                    case "runs" -> "run";
                    default -> text.toLowerCase(Locale.ROOT);
                };
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
                return List.of(text.split("\\s+"));
            }
        };
    }
}
