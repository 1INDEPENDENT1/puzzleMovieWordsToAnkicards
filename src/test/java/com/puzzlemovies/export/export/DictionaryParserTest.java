package com.puzzlemovies.export.export;

import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import org.junit.jupiter.api.Test;
import java.util.List;
import static com.puzzlemovies.export.export.DictionarySourceFieldsTest.*;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryParserTest {
    @Test void typedWordsPreserveSourceAndExistingNormalizerIdentity() {
        var result = PARSER.parseWordPages(List.of(page(DictionarySection.WORD, "words-real.html", 36)));
        assertEquals(36, result.records().size()); assertEquals(0, result.unparsedRecordCount());
        var word = result.records().get(0);
        assertEquals("to mince", word.sourceText()); assertEquals("to", word.matchingIdentity());
        assertTrue(word.translations().contains("смягчать"));
    }
    @Test void typedPhrasesRetainMetadataAndMatchingTokens() {
        var result = PARSER.parsePhrasePages(List.of(page(DictionarySection.PHRASE, "phrases-real.html", 36)));
        assertEquals(36, result.records().size()); assertEquals(0, result.unparsedRecordCount());
        var phrase = result.records().get(0);
        assertEquals("Man, heck of a win!", phrase.sourceText());
        assertTrue(phrase.matchingTokens().contains("win"));
        assertEquals("Тачки 3 / Cars 3", phrase.movieTitle());
        assertEquals("https://puzzle-movies.com/films/cars-3-2017", phrase.movieUrl());
    }
}
