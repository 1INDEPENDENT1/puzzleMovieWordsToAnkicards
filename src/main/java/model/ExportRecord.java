package model;

public record ExportRecord(String front, String backHtml, RecordType recordType) {
    public enum RecordType {
        WORD,
        PHRASE
    }
}
