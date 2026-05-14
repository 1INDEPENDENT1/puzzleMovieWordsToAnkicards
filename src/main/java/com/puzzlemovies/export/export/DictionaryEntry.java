package com.puzzlemovies.export.export;

public class DictionaryEntry {
    private final String source;
    private final String translation;

    public DictionaryEntry(String source, String translation) {
        this.source = source;
        this.translation = translation;
    }

    public String getSource() {
        return source;
    }

    public String getTranslation() {
        return translation;
    }
}
