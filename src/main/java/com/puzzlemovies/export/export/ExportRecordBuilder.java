package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExportRecordBuilder {
    public List<ExportRecord> buildWordRecords(List<DictionaryWord> words,
                                               Map<String, List<PhraseExample>> examplesByWord) {
        List<ExportRecord> records = new ArrayList<>();
        for (DictionaryWord word : words) {
            List<PhraseExample> examples = examplesByWord.getOrDefault(word.matchingIdentity(), List.of());
            records.add(new ExportRecord(
                    word.sourceText(),
                    buildBack(word.translations(), examples),
                    ExportRecord.RecordKind.WORD));
        }
        return records;
    }

    public List<ExportRecord> buildPhraseRecords(List<DictionaryPhrase> phrases) {
        List<ExportRecord> records = new ArrayList<>();
        for (DictionaryPhrase phrase : phrases) {
            records.add(new ExportRecord(
                    phrase.sourceText(),
                    buildBack(phrase.translations(), List.of()),
                    ExportRecord.RecordKind.PHRASE));
        }
        return records;
    }

    public List<ExportRecord> buildRecords(List<DictionaryWord> words,
                                           List<DictionaryPhrase> phrases,
                                           Map<String, List<PhraseExample>> examplesByWord,
                                           boolean includeWords,
                                           boolean includePhrases) {
        List<ExportRecord> records = new ArrayList<>();
        if (includeWords) {
            records.addAll(buildWordRecords(words, examplesByWord));
        }
        if (includePhrases) {
            records.addAll(buildPhraseRecords(phrases));
        }
        return records;
    }

    private String buildBack(Set<String> translations, List<PhraseExample> examples) {
        List<String> parts = new ArrayList<>();
        if (translations != null && !translations.isEmpty()) {
            parts.add(translations.stream()
                    .map(this::escapeHtml)
                    .collect(Collectors.joining("<br>")));
        }
        for (PhraseExample example : examples) {
            parts.add(renderExample(example));
        }
        return String.join("<br>", parts);
    }

    private String renderExample(PhraseExample example) {
        StringBuilder builder = new StringBuilder("<div class=\"example\">");
        builder.append(escapeHtml(example.phraseSourceText()));
        if (!example.phraseTranslations().isEmpty()) {
            builder.append("<br><span class=\"translation\">")
                    .append(example.phraseTranslations().stream()
                            .map(this::escapeHtml)
                            .collect(Collectors.joining("<br>")))
                    .append("</span>");
        }
        if (example.movieTitle() != null) {
            builder.append("<br><span class=\"movie\">");
            if (example.movieUrl() != null) {
                builder.append("<a href=\"")
                        .append(escapeHtmlAttribute(example.movieUrl()))
                        .append("\">")
                        .append(escapeHtml(example.movieTitle()))
                        .append("</a>");
            } else {
                builder.append(escapeHtml(example.movieTitle()));
            }
            builder.append("</span>");
        }
        builder.append("</div>");
        return builder.toString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("\"", "&quot;");
    }
}
