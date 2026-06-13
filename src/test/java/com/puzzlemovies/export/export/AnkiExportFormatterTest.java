package com.puzzlemovies.export.export;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnkiExportFormatterTest {
    @Test
    void formatsExactlyTwoTsvColumnsAndProtectsRowBoundaries() {
        AnkiExportFormatter formatter = new AnkiExportFormatter();
        List<ExportRecord> records = List.of(new ExportRecord(
                "front\ttext",
                "back\n<div>safe html</div>",
                ExportRecord.RecordKind.WORD));

        String formatted = formatter.formatTsv(records);

        String[] lines = formatted.split("\\R");
        assertEquals(1, lines.length);
        assertEquals(2, lines[0].split("\\t", -1).length);
        assertTrue(lines[0].contains("front text"));
        assertTrue(lines[0].contains("<div>safe html</div>"));
    }
}
