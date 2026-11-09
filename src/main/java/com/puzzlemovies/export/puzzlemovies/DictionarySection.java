package com.puzzlemovies.export.puzzlemovies;
public enum DictionarySection {
    WORD("word"), PHRASE("phrase");
    private final String item;
    DictionarySection(String item) { this.item = item; }
    public String item() { return item; }
}
