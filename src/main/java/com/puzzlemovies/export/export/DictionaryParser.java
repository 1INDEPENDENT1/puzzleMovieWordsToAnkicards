package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionaryPage;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.puzzlemovies.DictionaryPageInspector;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesDictionaryException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

@Component
public class DictionaryParser {
    private final VocabularyNormalizer normalizer;

    public DictionaryParser(VocabularyNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /** Extract source fields before linguistic normalization, retaining even malformed row positions. */
    public List<RawDictionaryEntry> parseSourceEntries(DictionaryPage page) {
        var inspection = page.inspection();
        if (!inspection.recognized() || !inspection.authenticated() || inspection.section() != page.section()
                || inspection.currentPage() != page.pageNumber()) {
            throw structureFailure(page);
        }
        Document document = Jsoup.parse(page.html(), page.sourceUri().toString());
        Element table = DictionaryPageInspector.dictionaryTable(document);
        if (table == null) throw structureFailure(page);
        // A single structural traversal means overlapping CSS classes never duplicate a source row.
        List<Element> rows = DictionaryPageInspector.sourceRows(table);
        if (rows.size() != inspection.sourceRecordCount()) throw structureFailure(page);
        List<RawDictionaryEntry> entries = new ArrayList<>();
        int rowNumber = 0;
        for (Element row : rows) {
            SourcePosition position = new SourcePosition(page.section(), page.pageNumber(), ++rowNumber,
                    optionalAttribute(row, "data-id"));
            List<Element> cells = row.children().stream().filter(child -> child.tagName().equals("td")).toList();
            Element firstCell = cells.isEmpty() ? null : cells.get(0);
            Element secondCell = cells.size() < 2 ? null : cells.get(1);
            if (page.section() == DictionarySection.WORD) {
                Element original = firstCell == null ? null : firstCell.selectFirst(".translate_line_without_popup");
                var metadata = sourceMetadata(secondCell);
                entries.add(new RawDictionaryEntry(position, sourceText(original),
                        sourceTranslations(firstCell, ".dict__video__list-table__word__translate"),
                        inlineExamples(secondCell, metadata), metadata.label(), metadata.title(), metadata.url()));
            } else {
                var metadata = sourceMetadata(firstCell);
                entries.add(new RawDictionaryEntry(position,
                        sourceText(secondCell == null ? null : secondCell.selectFirst(".dict__video__list-table__phrase__eng")),
                        sourceTranslations(secondCell, ".dict__video__list-table__phrase__rus"), List.of(),
                        metadata.label(), metadata.title(), metadata.url()));
            }
        }
        return List.copyOf(entries);
    }

    public DictionaryParseResult<DictionaryWord> parseWordPages(List<DictionaryPage> pages) {
        return parsePages(pages, DictionarySection.WORD, normalizer::normalizeSingle,
                value -> new DictionaryWord(value.entry().sourceText(), new LinkedHashSet<>(value.entry().translations()),
                        value.normalized().identity(), value.normalized().fallbackUsed(), List.of(value.entry())));
    }

    public DictionaryParseResult<DictionaryPhrase> parsePhrasePages(List<DictionaryPage> pages) {
        return parsePages(pages, DictionarySection.PHRASE, normalizer::normalizePhrase,
                value -> new DictionaryPhrase(value.entry().sourceText(), new LinkedHashSet<>(value.entry().translations()),
                        value.entry().movieTitle(), value.entry().movieUrl(), value.normalized().tokens(),
                        value.normalized().identity(), value.normalized().fallbackUsed(), value.normalized().sentences(),
                        List.of(value.entry())));
    }

    private <T> DictionaryParseResult<T> parsePages(List<DictionaryPage> pages, DictionarySection section,
            Function<String, VocabularyNormalizer.NormalizedText> normalize, Function<NormalizedEntry, T> create) {
        List<T> records = new ArrayList<>();
        List<RawDictionaryEntry> accepted = new ArrayList<>();
        List<ParseIssue> issues = new ArrayList<>();
        Set<Integer> seenPages = new LinkedHashSet<>();
        int sourceCount = 0;
        for (DictionaryPage page : pages) {
            if (page.section() != section || !seenPages.add(page.pageNumber())) throw structureFailure(page);
            for (RawDictionaryEntry entry : parseSourceEntries(page)) {
                sourceCount++;
                if (entry.sourceText().isBlank()) {
                    issues.add(new ParseIssue(entry.position(), ParseIssue.Reason.MISSING_ORIGINAL));
                    continue;
                }
                var normalized = normalize.apply(entry.sourceText());
                if (normalized.identity().isBlank()) {
                    issues.add(new ParseIssue(entry.position(), ParseIssue.Reason.UNUSABLE_IDENTITY));
                    continue;
                }
                records.add(create.apply(new NormalizedEntry(entry, normalized)));
                accepted.add(entry);
            }
        }
        return new DictionaryParseResult<>(records, accepted, issues, sourceCount);
    }

    private List<SourceExample> inlineExamples(Element cell, SourceMetadata metadata) {
        if (cell == null) return List.of();
        String originalSelector = ".dict__video__list-table__phrase__eng";
        String translationSelector = ".dict__video__list-table__phrase__rus";
        var originals = cell.select(originalSelector);
        if (!originals.isEmpty()) {
            List<Element> scopes = originals.stream().map(original -> inlineScope(original, cell, originalSelector)).toList();
            SourceMetadata shared = originals.size() == 1 ? metadata : sourceMetadata(cell,
                    field -> scopes.stream().noneMatch(scope -> scope == field || field.parents().contains(scope)));
            List<SourceExample> examples = new ArrayList<>();
            for (int index = 0; index < originals.size(); index++) {
                Element original = originals.get(index);
                Element scope = scopes.get(index);
                SourceMetadata local = sourceMetadata(scope);
                String sourceId = null;
                for (Element ancestor = original; ancestor != scope.parent() && sourceId == null; ancestor = ancestor.parent()) {
                    sourceId = optionalAttribute(ancestor, "data-id");
                }
                String text = sourceText(original);
                if (!text.isBlank()) {
                    examples.add(new SourceExample(text, inlineTranslations(cell, scope, originalSelector, translationSelector),
                            local.label() == null ? shared.label() : local.label(),
                            local.title() == null ? shared.title() : local.title(),
                            local.title() == null ? shared.url() : local.url(), sourceId));
                }
            }
            return List.copyOf(examples);
        }

        // The inline cell can also contain a plain-text context.
        Element content = cell.clone();
        content.select(translationSelector + ", .puzzle-card__phrase-source, a.dict__video__list-table__info__balloon__title").remove();
        String original = sourceText(content);
        return original.isBlank() ? List.of() : List.of(new SourceExample(original,
                sourceTranslations(cell, translationSelector),
                metadata.label(), metadata.title(), metadata.url(), optionalAttribute(cell, "data-id")));
    }

    private Element inlineScope(Element original, Element cell, String selector) {
        Element scope = original;
        while (scope != cell && scope.parent().select(selector).size() == 1) {
            scope = scope.parent();
        }
        return scope;
    }

    private List<String> inlineTranslations(Element cell, Element scope, String originalSelector, String translationSelector) {
        List<String> translations = new ArrayList<>(sourceTranslations(scope, translationSelector));
        if (scope != cell) {
            for (Element sibling = scope.nextElementSibling();
                 sibling != null && sibling.selectFirst(originalSelector) == null;
                 sibling = sibling.nextElementSibling()) {
                translations.addAll(sourceTranslations(sibling, translationSelector));
            }
        }
        return List.copyOf(translations);
    }

    private SourceMetadata sourceMetadata(Element cell) {
        return sourceMetadata(cell, field -> true);
    }

    private SourceMetadata sourceMetadata(Element cell, Predicate<Element> include) {
        if (cell == null) return new SourceMetadata(null, null, null);
        Element label = cell.select(".puzzle-card__phrase-source").stream().filter(include).findFirst().orElse(null);
        Element link = cell.select("a.dict__video__list-table__info__balloon__title").stream().filter(include).findFirst().orElse(null);
        String href = link == null ? null : optionalAttribute(link, "href");
        return new SourceMetadata(optionalText(label), optionalText(link),
                href == null ? null : link.absUrl("href"));
    }

    private List<String> sourceTranslations(Element cell, String selector) {
        if (cell == null) return List.of();
        List<String> values = new ArrayList<>();
        for (Element translation : cell.select(selector)) {
            String value = sourceText(translation);
            if (!value.isBlank()) values.add(value);
        }
        return List.copyOf(values);
    }

    private String sourceText(Element element) {
        if (element == null) return "";
        Element visible = element.clone();
        visible.select("[hidden], [aria-hidden=true], .translate_line_with_popup, [style~=display\\s*:\\s*none]").remove();
        // wholeText preserves the existing text-node boundaries around punctuation and inline spans.
        return visible.wholeText().replaceAll("[\\s\\u00a0]+", " ").trim();
    }

    private String optionalText(Element element) {
        String value = sourceText(element);
        return value.isBlank() ? null : value;
    }

    private String optionalAttribute(Element element, String name) {
        String value = element.attr(name).trim();
        return value.isBlank() ? null : value;
    }

    private PuzzleMoviesDictionaryException structureFailure(DictionaryPage page) {
        return new PuzzleMoviesDictionaryException(PuzzleMoviesDictionaryException.Category.STRUCTURE,
                page.section(), page.pageNumber(), null);
    }

    private record NormalizedEntry(RawDictionaryEntry entry, VocabularyNormalizer.NormalizedText normalized) { }
    private record SourceMetadata(String label, String title, String url) { }
}
