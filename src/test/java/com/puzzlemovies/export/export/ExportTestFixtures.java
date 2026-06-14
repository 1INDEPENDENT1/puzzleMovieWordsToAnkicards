package com.puzzlemovies.export.export;

import org.drugov.lingua.morph.Lemmatizer;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ExportTestFixtures {
    public static final String WORD_PAGE = """
            <html><body>
              <table>
                <tr><td>Run</td><td>бежать</td></tr>
                <tr><td>empty translation</td><td></td></tr>
              </table>
              <div class="dictionary__item">
                <span class="dictionary__word">Moon</span>
                <span class="dictionary__translation">луна</span>
              </div>
            </body></html>
            """;

    public static final String PHRASE_PAGE = """
            <html><body>
              <table>
                <tr>
                  <td>I am running home.</td>
                  <td>Я бегу домой.</td>
                  <td><a href="/movie/1">Arrival</a></td>
                </tr>
              </table>
              <div class="dictionary__item">
                <span class="dictionary__phrase">The moon is bright.</span>
                <span class="dictionary__translation">Луна яркая.</span>
                <span class="dictionary__movie"><a href="https://puzzle-movies.com/movie/2">Moon</a></span>
              </div>
            </body></html>
            """;

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

    public static List<String> wordPages() {
        return List.of(WORD_PAGE);
    }

    public static List<String> phrasePages() {
        return List.of(PHRASE_PAGE);
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
