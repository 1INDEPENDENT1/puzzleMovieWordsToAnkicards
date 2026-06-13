package com.puzzlemovies.export.export;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnkiExportFormatter {
    public String formatTsv(List<ExportRecord> records) {
        StringBuilder builder = new StringBuilder();
        for (ExportRecord record : records) {
            builder.append(sanitize(record.front()))
                    .append('\t')
                    .append(sanitize(record.back()))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }
}
