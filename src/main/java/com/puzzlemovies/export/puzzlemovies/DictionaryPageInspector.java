package com.puzzlemovies.export.puzzlemovies;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryException.Category.*;

/** Recognizes the observed dictionary shell; vocabulary field extraction belongs to the parser. */
public class DictionaryPageInspector {
    public DictionaryPage.PageInspection inspect(DictionarySection section, int pageNumber, String html) {
        return inspect(section, pageNumber, URI.create("https://puzzle-movies.com/dictionary"), html);
    }

    public DictionaryPage.PageInspection inspect(DictionarySection section, int pageNumber, URI sourceUri, String html) {
        if (html == null || html.isBlank()) throw failure(STRUCTURE, section, pageNumber);
        Document doc = Jsoup.parse(html);
        if (!doc.select(".movies-header__user-anon, [onclick*=show_sign_popup]").isEmpty())
            throw new PuzzleMoviesSessionExpiredException(section, pageNumber);
        var tabs = doc.select(".movies-tabs__item.is-active");
        Element table = dictionaryTable(doc);
        if (tabs.size() != 1 || table == null) throw failure(STRUCTURE, section, pageNumber);
        URI tabUri = resolve(sourceUri, tabs.first().attr("href"), section, pageNumber);
        if (!section.item().equals(parameter(tabUri, "item", section, pageNumber))) throw failure(STRUCTURE, section, pageNumber);
        Element totalElement = tabs.first().selectFirst(".movies-tabs__item-title span");
        if (totalElement == null) throw failure(STRUCTURE, section, pageNumber);
        int total = number(totalElement.text().replaceAll("[\\s\\u00a0\\u202f]", ""), STRUCTURE, section, pageNumber);
        var rows = sourceRows(table);
        for (Element row : rows) {
            if (row.closest("table") != table) throw failure(STRUCTURE, section, pageNumber);
            if (section == DictionarySection.WORD && !row.select(".dict__video__list-table__phrase__eng, .dictionary-phrase__eng").isEmpty())
                throw failure(STRUCTURE, section, pageNumber);
            if (section == DictionarySection.PHRASE && !row.select(".translate_line_without_popup").isEmpty())
                throw failure(STRUCTURE, section, pageNumber);
        }
        // Unrecognized table rows cannot silently disappear from completeness accounting.
        for (Element row : table.select("tr")) {
            if (row.closest("table") == table && !row.select("td").isEmpty() && !row.hasClass("dict__video__list-table__row"))
                throw failure(STRUCTURE, section, pageNumber);
        }
        var current = doc.select(".paginator-style-2__element.is-current");
        int currentPage;
        if (current.isEmpty() && doc.select(".paginator-style-2").isEmpty() && pageNumber == 1) currentPage = 1;
        else if (current.size() == 1) currentPage = number(current.first().text(), PAGINATION, section, pageNumber);
        else throw failure(PAGINATION, section, pageNumber);
        if (currentPage != pageNumber) throw failure(PAGINATION, section, pageNumber);
        Integer nextPage = null;
        for (Element link : doc.select(".paginator-style-2__element a[href]")) {
            URI target = resolve(sourceUri, link.attr("href"), section, pageNumber);
            String pageValue = parameter(target, "page", section, pageNumber);
            if (pageValue == null) throw failure(PAGINATION, section, pageNumber);
            int candidate = number(pageValue, PAGINATION, section, pageNumber);
            if (candidate < 1) throw failure(PAGINATION, section, pageNumber);
            String item = parameter(target, "item", section, pageNumber);
            if (item != null && !section.item().equals(item)) throw failure(PAGINATION, section, pageNumber);
            String dataPage = link.attr("data-page");
            if (!dataPage.isBlank() && number(dataPage, PAGINATION, section, pageNumber) != candidate)
                throw failure(PAGINATION, section, pageNumber);
            if (link.closest(".paginator-style-2__right") != null && candidate != currentPage + 1)
                throw failure(PAGINATION, section, pageNumber);
            if (candidate > currentPage && (nextPage == null || candidate < nextPage)) nextPage = candidate;
        }
        if (nextPage != null && nextPage != currentPage + 1) throw failure(PAGINATION, section, pageNumber);
        if ((total > 0 && rows.isEmpty()) || (total == 0 && rows.isEmpty() && (nextPage != null || currentPage != 1)))
            throw failure(PAGINATION, section, pageNumber);
        return new DictionaryPage.PageInspection(section, total, currentPage, nextPage, rows.size(), true, true);
    }

    /** The observed active switcher owns dictionary rows; other page tables are unrelated. */
    public static Element dictionaryTable(Document doc) {
        var tables = doc.select("#dictionary-switcher .active table.dict__video__list-table");
        if (tables.isEmpty()) tables = doc.select("table.dict__video__list-table");
        return tables.size() == 1 ? tables.first() : null;
    }

    public static List<Element> sourceRows(Element table) {
        return table.select("tr.dict__video__list-table__row").stream()
                .filter(row -> row.closest("table") == table).toList();
    }

    private URI resolve(URI source, String href, DictionarySection section, int page) {
        try {
            URI target = source.resolve(href);
            if (!sameOrigin(source, target) || !"/dictionary".equals(target.getPath()) || target.getRawUserInfo() != null || target.getRawFragment() != null)
                throw failure(PAGINATION, section, page);
            return target;
        } catch (IllegalArgumentException ex) { throw failure(PAGINATION, section, page); }
    }
    static boolean sameOrigin(URI left, URI right) {
        return left.getScheme() != null && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost() != null && left.getHost().equalsIgnoreCase(right.getHost()) && port(left) == port(right);
    }
    private static int port(URI uri) { return uri.getPort() == -1 ? ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80) : uri.getPort(); }
    private String parameter(URI uri, String key, DictionarySection section, int page) {
        List<String> matches = new ArrayList<>();
        try {
            for (String part : (uri.getRawQuery() == null ? "" : uri.getRawQuery()).split("&")) {
                String[] pair = part.split("=", 2);
                if (URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(key))
                    matches.add(pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
        } catch (IllegalArgumentException ex) { throw failure(PAGINATION, section, page); }
        if (matches.size() > 1) throw failure(PAGINATION, section, page);
        return matches.isEmpty() ? null : matches.getFirst();
    }
    private int number(String value, PuzzleMoviesDictionaryException.Category category, DictionarySection section, int page) {
        try { if (!value.matches("[0-9]+")) throw new NumberFormatException(); return Integer.parseInt(value); }
        catch (NumberFormatException ex) { throw failure(category, section, page); }
    }
    private PuzzleMoviesDictionaryException failure(PuzzleMoviesDictionaryException.Category category, DictionarySection section, int page) {
        return new PuzzleMoviesDictionaryException(category, section, page, null);
    }
}
