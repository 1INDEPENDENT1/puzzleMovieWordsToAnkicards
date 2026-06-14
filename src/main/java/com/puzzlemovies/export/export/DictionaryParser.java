package com.puzzlemovies.export.export;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class DictionaryParser {
    private final VocabularyNormalizer normalizer;

    public DictionaryParser(VocabularyNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public List<DictionaryWord> parseWords(List<String> pages) {
        List<DictionaryWord> words = new ArrayList<>();
        for (String page : pages) {
            Document document = Jsoup.parse(page, "https://puzzle-movies.com");
            for (RawEntry entry : parseEntries(document, true)) {
                VocabularyNormalizer.NormalizedText normalized = normalizer.normalizeSingle(entry.sourceText());
                if (normalized.identity().isBlank()) {
                    continue;
                }
                words.add(new DictionaryWord(
                        entry.sourceText(),
                        translations(entry.translation()),
                        normalized.identity(),
                        normalized.fallbackUsed()));
            }
        }
        return words;
    }

    public List<DictionaryPhrase> parsePhrases(List<String> pages) {
        List<DictionaryPhrase> phrases = new ArrayList<>();
        for (String page : pages) {
            Document document = Jsoup.parse(page, "https://puzzle-movies.com");
            for (RawEntry entry : parseEntries(document, false)) {
                VocabularyNormalizer.NormalizedText normalized = normalizer.normalizePhrase(entry.sourceText());
                if (normalized.identity().isBlank()) {
                    continue;
                }
                phrases.add(new DictionaryPhrase(
                        entry.sourceText(),
                        translations(entry.translation()),
                        entry.movieTitle(),
                        entry.movieUrl(),
                        normalized.tokens(),
                        normalized.identity(),
                        normalized.fallbackUsed()));
            }
        }
        return phrases;
    }

    private List<RawEntry> parseEntries(Document document, boolean words) {
        List<RawEntry> entries = new ArrayList<>();
        entries.addAll(parseTableRows(document, words));
        entries.addAll(parseCardRows(document, words));
        return entries;
    }

    private List<RawEntry> parseTableRows(Document document, boolean words) {
        List<RawEntry> entries = new ArrayList<>();
        Elements rows = document.select("table tr:has(td), .dictionary__row:has(td)");
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.isEmpty()) {
                continue;
            }
            String source = text(cells.get(0));
            if (source.isBlank()) {
                continue;
            }
            String translation = cells.size() > 1 ? text(cells.get(1)) : "";
            MovieInfo movieInfo = words ? MovieInfo.empty() : extractMovieInfo(row);
            entries.add(new RawEntry(source, translation, movieInfo.title(), movieInfo.url()));
        }
        return entries;
    }

    private List<RawEntry> parseCardRows(Document document, boolean words) {
        List<RawEntry> entries = new ArrayList<>();
        Elements cards = document.select(".dictionary__item, .dictionary__list-item, .dictionary-item, .entry, .word-item, .phrase-item");
        for (Element card : cards) {
            String source = selectText(card,
                    ".dictionary__source",
                    words ? ".dictionary__word" : ".dictionary__phrase",
                    words ? ".word" : ".phrase",
                    ".source",
                    ".original");
            if (source.isBlank()) {
                continue;
            }
            String translation = selectText(card,
                    ".dictionary__target",
                    ".dictionary__translation",
                    ".translation",
                    ".meaning",
                    ".translated");
            MovieInfo movieInfo = words ? MovieInfo.empty() : extractMovieInfo(card);
            entries.add(new RawEntry(source, translation, movieInfo.title(), movieInfo.url()));
        }
        return entries;
    }

    private MovieInfo extractMovieInfo(Element row) {
        Element link = row.selectFirst(".movie a[href], .dictionary__movie a[href], a[href*='/movie'], a[href*='/film']");
        if (link == null) {
            return MovieInfo.empty();
        }
        String title = text(link);
        String url = link.absUrl("href");
        if (url == null || url.isBlank()) {
            url = link.attr("href").trim();
        }
        return new MovieInfo(title, url);
    }

    private String selectText(Element row, String... selectors) {
        for (String selector : selectors) {
            Element element = row.selectFirst(selector);
            if (element != null) {
                String text = text(element);
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private Set<String> translations(String translation) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (translation != null && !translation.isBlank()) {
            values.add(translation.trim());
        }
        return values;
    }

    private String text(Element element) {
        return element == null ? "" : element.text().trim();
    }

    private record RawEntry(String sourceText, String translation, String movieTitle, String movieUrl) {
    }

    private record MovieInfo(String title, String url) {
        private static MovieInfo empty() {
            return new MovieInfo(null, null);
        }
    }
}
