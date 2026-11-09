package com.puzzlemovies.export.service;

import com.puzzlemovies.export.puzzlemovies.SourceCountDiscrepancy;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

public record ExportCompletionInfo(Completeness completeness, int unparsedRecordCount,
                                   List<SourceCountDiscrepancy> sourceCountDiscrepancies, String warningMessage) {
    public enum Completeness { FULL, PARTIAL }
    public ExportCompletionInfo {
        sourceCountDiscrepancies = List.copyOf(sourceCountDiscrepancies);
        var sections = new HashSet<>();
        if (sourceCountDiscrepancies.stream().anyMatch(d -> !sections.add(d.section())))
            throw new IllegalArgumentException("Repeated section counter warning");
        boolean hasWarnings = unparsedRecordCount > 0 || !sourceCountDiscrepancies.isEmpty();
        if (completeness == null || unparsedRecordCount < 0 || (completeness == Completeness.PARTIAL) != hasWarnings)
            throw new IllegalArgumentException("Invalid completion count");
        var warnings = new ArrayList<String>();
        if (unparsedRecordCount > 0) warnings.add(unparsedRecordCount + " source record(s) could not be parsed.");
        for (var count : sourceCountDiscrepancies) warnings.add("Source counter mismatch (" + count.section()
                + "): site reports " + count.declaredTotal() + ", but all pages returned " + count.retrievedTotal()
                + " source records. The export contains the available records.");
        warningMessage = hasWarnings ? "Partial export: " + String.join(" ", warnings) : null;
    }
    public static ExportCompletionInfo full() { return new ExportCompletionInfo(Completeness.FULL, 0, List.of(), null); }
    public static ExportCompletionInfo partial(int count) { return new ExportCompletionInfo(Completeness.PARTIAL, count, List.of(), null); }
    public static ExportCompletionInfo withWarnings(int rejected, List<SourceCountDiscrepancy> discrepancies) {
        return new ExportCompletionInfo(rejected > 0 || !discrepancies.isEmpty() ? Completeness.PARTIAL : Completeness.FULL,
                rejected, discrepancies, null);
    }
}
