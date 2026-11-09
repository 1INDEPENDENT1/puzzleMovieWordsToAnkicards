package com.puzzlemovies.export.service;
import java.util.List;
import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.review.ReviewExportSource;
public record GeneratedExportData(List<ExportRecord> records, ReviewExportSource reviewSource,
                                  List<RawDictionaryEntry> sourceEntries, ExportCompletionInfo completion) {
    public GeneratedExportData { records = List.copyOf(records); sourceEntries = List.copyOf(sourceEntries); }
}
