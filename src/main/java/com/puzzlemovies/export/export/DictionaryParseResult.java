package com.puzzlemovies.export.export;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
public record DictionaryParseResult<T>(List<T> records, List<RawDictionaryEntry> acceptedSourceEntries,
                                       List<ParseIssue> issues, int sourceRecordCount) {
    public DictionaryParseResult {
        records = List.copyOf(records); acceptedSourceEntries = List.copyOf(acceptedSourceEntries); issues = List.copyOf(issues);
        Set<SourcePosition> accepted = new HashSet<>();
        for (var entry : acceptedSourceEntries) {
            if (!accepted.add(entry.position())) throw new IllegalArgumentException("Repeated accepted source position");
        }
        Set<SourcePosition> rejected = new HashSet<>();
        for (var issue : issues) rejected.add(issue.position());
        if (sourceRecordCount < 0 || records.size() != acceptedSourceEntries.size()
                || sourceRecordCount != accepted.size() + rejected.size()
                || rejected.stream().anyMatch(accepted::contains))
            throw new IllegalArgumentException("Dictionary source accounting mismatch");
    }
    public int unparsedRecordCount() { return (int) issues.stream().map(ParseIssue::position).distinct().count(); }
    public static <T> DictionaryParseResult<T> empty() { return new DictionaryParseResult<>(List.of(), List.of(), List.of(), 0); }
}
