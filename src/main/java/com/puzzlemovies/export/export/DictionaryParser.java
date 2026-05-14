package com.puzzlemovies.export.export;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DictionaryParser {
    public List<DictionaryEntry> parseWords(List<String> pages) {
        return parseEntries(pages);
    }

    public List<DictionaryEntry> parsePhrases(List<String> pages) {
        return parseEntries(pages);
    }

    private List<DictionaryEntry> parseEntries(List<String> pages) {
        List<DictionaryEntry> entries = new ArrayList<>();
        for (String page : pages) {
            Document document = Jsoup.parse(page);
            entries.addAll(parseTableRows(document));
            entries.addAll(parseCardRows(document));
        }
        return entries;
    }

    private List<DictionaryEntry> parseTableRows(Document document) {
        List<DictionaryEntry> entries = new ArrayList<>();
        Elements rows = document.select("table tr");
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }
            String source = cells.get(0).text().trim();
            String translation = cells.get(1).text().trim();
            if (!source.isEmpty() && !translation.isEmpty()) {
                entries.add(new DictionaryEntry(source, translation));
            }
        }
        return entries;
    }

    private List<DictionaryEntry> parseCardRows(Document document) {
        List<DictionaryEntry> entries = new ArrayList<>();
        Elements cards = document.select(".dictionary-item, .entry, .word-item, .phrase-item");
        for (Element card : cards) {
            Element sourceEl = firstNonEmpty(card, ".source", ".word", ".phrase", ".original");
            Element translationEl = firstNonEmpty(card, ".translation", ".meaning", ".translated");
            if (sourceEl == null || translationEl == null) {
                continue;
            }
            String source = sourceEl.text().trim();
            String translation = translationEl.text().trim();
            if (!source.isEmpty() && !translation.isEmpty()) {
                entries.add(new DictionaryEntry(source, translation));
            }
        }
        return entries;
    }

    private Element firstNonEmpty(Element root, String... selectors) {
        for (String selector : selectors) {
            Element element = root.selectFirst(selector);
            if (element != null && !element.text().isBlank()) {
                return element;
            }
        }
        return null;
    }
}
