package parser;

import model.DictionaryPhrase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class PhraseParser {
    public List<DictionaryPhrase> parsePage(String html) {
        Document document = Jsoup.parse(html);
        Elements rows = document.select(".dictionary__row, .dictionary__item, .dictionary__list-item, table tr:has(td)");
        List<DictionaryPhrase> phrases = new ArrayList<>();
        for (Element row : rows) {
            String english = selectText(row, ".dictionary__source", ".dictionary__phrase", ".phrase", "td:nth-of-type(1)");
            String russian = selectText(row, ".dictionary__target", ".dictionary__translation", ".translation", "td:nth-of-type(2)");
            MovieInfo movieInfo = extractMovieInfo(row);
            if (english == null || english.isBlank()) {
                continue;
            }
            phrases.add(DictionaryPhrase.fromRaw(english, russian, movieInfo.title, movieInfo.url));
        }
        return phrases;
    }

    private MovieInfo extractMovieInfo(Element row) {
        Element link = row.selectFirst("a[href]");
        if (link == null) {
            return new MovieInfo("", "");
        }
        String title = link.text().trim();
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
                String text = element.text().trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private record MovieInfo(String title, String url) {}
}
