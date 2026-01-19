package export;

import model.CliOptions;
import model.DictionaryPhrase;
import model.DictionaryWord;
import model.ExportRecord;
import model.PhraseExample;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExportFormatter {
    public List<ExportRecord> buildRecords(List<DictionaryWord> words, List<DictionaryPhrase> phrases) {
        return buildRecords(words, phrases, Map.of());
    }

    public List<ExportRecord> buildRecords(List<DictionaryWord> words,
                                           List<DictionaryPhrase> phrases,
                                           Map<String, List<PhraseExample>> examplesByWordKey) {
        List<ExportRecord> records = new ArrayList<>();
        for (DictionaryWord word : words) {
            List<PhraseExample> examples = examplesByWordKey.getOrDefault(word.normalizedKey(), List.of());
            String back = buildBackHtml(word.russian(), examples);
            records.add(new ExportRecord(word.english(), back, ExportRecord.RecordType.WORD));
        }
        for (DictionaryPhrase phrase : phrases) {
            String back = buildBackHtml(phrase.russian(), List.of());
            records.add(new ExportRecord(phrase.english(), back, ExportRecord.RecordType.PHRASE));
        }
        return records;
    }

    public String format(List<ExportRecord> records, CliOptions.ExportFormat format) {
        String delimiter = format == CliOptions.ExportFormat.CSV ? "," : "\t";
        StringBuilder builder = new StringBuilder();
        for (ExportRecord record : records) {
            String front = sanitize(record.front());
            String back = sanitize(record.backHtml());
            if (format == CliOptions.ExportFormat.CSV) {
                builder.append(escapeCsv(front)).append(delimiter).append(escapeCsv(back));
            } else {
                builder.append(front).append(delimiter).append(back);
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\t", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private String escapeCsv(String value) {
        String cleaned = value == null ? "" : value;
        boolean needsQuotes = cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n") || cleaned.contains("\r");
        String escaped = cleaned.replace("\"", "\"\"");
        if (needsQuotes) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String buildBackHtml(String translation, List<PhraseExample> examples) {
        StringBuilder builder = new StringBuilder();
        String cleanedTranslation = translation == null ? "" : translation.trim();
        if (!cleanedTranslation.isBlank()) {
            builder.append(escapeHtml(cleanedTranslation));
        }
        if (examples != null && !examples.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("<br>");
            }
            for (PhraseExample example : examples) {
                builder.append("<div>");
                builder.append(escapeHtml(example.phraseEnglish()));
                if (example.phraseRussian() != null && !example.phraseRussian().isBlank()) {
                    builder.append(" — ").append(escapeHtml(example.phraseRussian()));
                }
                if (example.movieTitle() != null && !example.movieTitle().isBlank()) {
                    builder.append(" (");
                    if (example.movieUrl() != null && !example.movieUrl().isBlank()) {
                        builder.append("<a href=\"")
                                .append(escapeHtml(example.movieUrl()))
                                .append("\">")
                                .append(escapeHtml(example.movieTitle()))
                                .append("</a>");
                    } else {
                        builder.append(escapeHtml(example.movieTitle()));
                    }
                    builder.append(")");
                }
                builder.append("</div>");
            }
        }
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
}
