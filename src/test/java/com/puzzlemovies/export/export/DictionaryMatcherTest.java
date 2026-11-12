package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import org.jsoup.nodes.Entities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DictionaryMatcherTest {
    @ParameterizedTest
    @CsvSource(value = {
            "give up now | Give up now. | 1",
            "give up now | Give right up now. | 1",
            "give up now | Give this right back up immediately right here now. | 1",
            "give up now | Give this right back up again to me right now. | 0",
            "give up now | Give this right back away up now. | 0",
            "give up | Give, please, up. | 1",
            "give up | Give one two three, up. | 1",
            "give up | Give. up. | 0",
            "give up | 'Give!' Up you go. | 0",
            "i run | You and I. Run home. | 0",
            "give up | Up and give. | 0",
            "give up | Please give. | 0",
            "go go | Go. | 0",
            "go go | Go, go, go. | 1",
            "give up now | Give up one two up right now. | 1",
            "give up now | Give up one two three up now. | 0",
            "he | The answer. | 0",
            "give up | Give Dr. Green up. | 1",
            "give up | Give 3.5 points up. | 1"
    }, delimiter = '|', quoteCharacter = '`')
    void matchesAllPartsInOrderWithBoundedGapsWithinOneSentence(String word, String phrase, int expectedMatches) {
        var words = parseWords(List.of(word));
        var phrases = parsePhrases(List.of(phrase), List.of("First film"));

        var matches = new DictionaryMatcher().matchAll(words, phrases);

        assertEquals(expectedMatches, matches.forWord(words.getFirst()).size());
    }

    @Test
    void numericWordsAlsoCountTowardTheGapWithTheRealLemmatizer() {
        var normalizer = new VocabularyNormalizer(new org.drugov.lingua.morph.LuceneLemmatizer());
        var word = normalizer.normalizeSingle("to give up");
        var examples = List.of(normalizer.normalizePhrase("Give 3.5 points back up."),
                normalizer.normalizePhrase("Give 3.5 more points back up."));

        assertEquals(List.of(List.of(0)), new DictionaryMatcher().matchingExampleIndexes(List.of(word), examples));
    }

    @Test
    void matchesIndependentZeroOneTwoFiveAndHundredExampleReference() throws IOException {
        var expectedLinks = readFixture("expected-links.txt");
        List<String> words = expectedLinks.stream().map(row -> row[0]).toList();
        var rows = readFixture("phrases.txt");
        var phrases = parsePhrases(rows.stream().map(row -> row[1]).toList(),
                rows.stream().map(row -> row[2]).toList());
        var matches = new DictionaryMatcher().matchAll(parseWords(words), phrases);

        for (var expected : expectedLinks) {
            var actualIds = matches.forWord(expected[0]).stream().map(example ->
                    rows.stream().filter(row -> row[1].equals(example.phraseSourceText())
                                    && row[2].equals(example.movieTitle()))
                            .findFirst().orElseThrow()[0]).toList();
            var expectedIds = expected[1].isEmpty() ? List.of() : List.of(expected[1].split(","));
            assertEquals(expectedIds, actualIds, expected[0]);
        }
    }

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

    private List<DictionaryWord> parseWords(List<String> words) {
        var parser = new DictionaryParser(new VocabularyNormalizer(ExportTestFixtures.testLemmatizer()));
        String html = "<table class='dict__video__list-table'>" + words.stream()
                .map(word -> ExportTestFixtures.wordRow(Entities.escape(word), ""))
                .collect(java.util.stream.Collectors.joining()) + "</table>";
        return parser.parseWordPages(List.of(ExportTestFixtures.page(DictionarySection.WORD, html, words.size()))).records();
    }

    private List<DictionaryPhrase> parsePhrases(List<String> phrases, List<String> movieTitles) {
        var parser = new DictionaryParser(new VocabularyNormalizer(ExportTestFixtures.testLemmatizer()));
        StringBuilder html = new StringBuilder("<table class='dict__video__list-table'>");
        for (int index = 0; index < phrases.size(); index++) {
            String movie = "<a class='dict__video__list-table__info__balloon__title' href='/movie/"
                    + movieTitles.get(index).replace(" ", "-") + "'>" + Entities.escape(movieTitles.get(index)) + "</a>";
            html.append(ExportTestFixtures.phraseRow(Entities.escape(phrases.get(index)), "", movie));
        }
        html.append("</table>");
        return parser.parsePhrasePages(List.of(ExportTestFixtures.page(DictionarySection.PHRASE, html.toString(), phrases.size()))).records();
    }

    private List<String[]> readFixture(String filename) throws IOException {
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/fixtures/linked-examples/" + filename))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .skip(1).map(line -> line.split("\\t", -1)).toList();
        }
    }
}
