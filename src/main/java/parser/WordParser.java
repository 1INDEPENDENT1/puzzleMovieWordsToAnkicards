package parser;

import model.DictionaryWord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class WordParser {
    public List<DictionaryWord> parsePage(String html) {
        Document document = Jsoup.parse(html);
        Elements rows = document.select(".dictionary__row, .dictionary__item, .dictionary__list-item, table tr:has(td)");
        List<DictionaryWord> words = new ArrayList<>();
        for (Element row : rows) {
            String english = selectText(row, ".dictionary__source", ".dictionary__word", ".word", "td:nth-of-type(1)");
            String russian = selectText(row, ".dictionary__target", ".dictionary__translation", ".translation", "td:nth-of-type(2)");
            if (english == null || english.isBlank()) {
                continue;
            }
            words.add(DictionaryWord.fromRaw(english, russian));
        }
        return words;
    }

    private String selectText(Element row, String... selectors) {
        for (String selector : selectors) {
            Element element = row.selectFirst(selector);
            if (element != null) {
                String text = element.text().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
