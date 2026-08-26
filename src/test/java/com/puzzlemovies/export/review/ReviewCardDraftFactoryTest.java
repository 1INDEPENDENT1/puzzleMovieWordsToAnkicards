package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.CompleteWordExampleMatches;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;
import com.puzzlemovies.export.export.PhraseExample;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewCardDraftFactoryTest {
    private final ReviewCardDraftFactory factory = new ReviewCardDraftFactory();

    @Test
    void mapsWordRecordWithGeneratedExampleIntoContextualDraft() {
        List<ReviewCardDraft> drafts = factory.createDrafts(ReviewTestFixtures.wordRecordWithExample());

        assertEquals(1, drafts.size());
        ReviewCardDraft draft = drafts.get(0);
        assertEquals("Run", draft.originalText());
        assertEquals("I am running home.", draft.instanceText());
        assertTrue(draft.translationText().contains("бежать"));
        assertTrue(draft.translationText().contains("Я бегу домой."));
        assertEquals("Arrival", draft.sourceContext());
        assertEquals(ExportRecord.RecordKind.WORD, draft.recordKind());
    }

    @Test
    void mapsStandalonePhraseAndPreservesBlankTranslation() {
        ReviewCardDraft phrase = factory.createDrafts(ReviewTestFixtures.phraseRecord()).get(0);
        ReviewCardDraft blank = factory.createDrafts(new ExportRecord("empty translation", "", ExportRecord.RecordKind.WORD)).get(0);

        assertEquals("The moon is bright.", phrase.originalText());
        assertNull(phrase.instanceText());
        assertEquals("Луна яркая.", phrase.translationText());
        assertEquals("", blank.translationText());
    }

    @Test
    void createsSeparateDraftsForMultipleGeneratedExamples() {
        ExportRecord record = new ExportRecord(
                "Turn",
                "поворачивать<br><div class=\"example\">Turn left.<br><span class=\"translation\">Поверни налево.</span></div>"
                        + "<div class=\"example\">Turn around.<br><span class=\"translation\">Обернись.</span></div>",
                ExportRecord.RecordKind.WORD);

        List<ReviewCardDraft> drafts = factory.createDrafts(record);

        assertEquals(2, drafts.size());
        assertEquals("Turn left.", drafts.get(0).instanceText());
        assertEquals("Turn around.", drafts.get(1).instanceText());
    }

    @Test
    void createsContextualWordOnlyUnmatchedAndOverflowDraftsFromStructuredSource() {
        DictionaryWord run = new DictionaryWord("Run", Set.of("бежать"), "run", false);
        DictionaryWord ocean = new DictionaryWord("Ocean", Set.of("океан"), "ocean", false);
        DictionaryPhrase first = phrase("I run home.", "Я бегу домой.", "run");
        DictionaryPhrase second = phrase("Run fast.", "Беги быстро.", "run");
        DictionaryPhrase overflow = phrase("Runs daily.", "Бегает каждый день.", "run");
        DictionaryPhrase unmatched = phrase("The moon is bright.", "", "moon");
        CompleteWordExampleMatches matches = new CompleteWordExampleMatches(Map.of("run", List.of(
                example("run", first), example("run", second), example("run", overflow))));

        List<ReviewCardDraft> drafts = factory.createDrafts(new ReviewExportSource(
                List.of(run, ocean), List.of(first, second, overflow, unmatched), matches));

        assertEquals(5, drafts.size());
        assertEquals("Run", drafts.get(0).originalText());
        assertEquals("I run home.", drafts.get(0).instanceText());
        assertTrue(drafts.get(0).translationText().contains("бежать"));
        assertTrue(drafts.get(0).translationText().contains("Я бегу домой."));
        assertEquals("Ocean", drafts.get(2).originalText());
        assertNull(drafts.get(2).instanceText());
        assertEquals("Runs daily.", drafts.get(3).originalText());
        assertNull(drafts.get(3).instanceText());
        assertEquals("The moon is bright.", drafts.get(4).originalText());
        assertEquals("", drafts.get(4).translationText());
    }

    @Test
    void retainsContextForEveryEligibleWordAndOnlyOneStandaloneOverflowDraft() {
        DictionaryWord run = new DictionaryWord("Run", Set.of(), "run", false);
        DictionaryWord home = new DictionaryWord("Home", Set.of(), "home", false);
        DictionaryPhrase shared = phrase("Run home.", "Домой.", "run", "home");
        DictionaryPhrase homeFirst = phrase("Home first.", "Первый дом.", "home");
        DictionaryPhrase homeSecond = phrase("Home second.", "Второй дом.", "home");
        CompleteWordExampleMatches matches = new CompleteWordExampleMatches(Map.of(
                "run", List.of(example("run", shared)),
                "home", List.of(example("home", homeFirst), example("home", homeSecond), example("home", shared))));

        List<ReviewCardDraft> drafts = factory.createDrafts(new ReviewExportSource(
                List.of(run, home), List.of(shared, homeFirst, homeSecond), matches));

        assertEquals(4, drafts.size());
        assertTrue(drafts.stream().anyMatch(draft -> draft.originalText().equals("Run")
                && draft.instanceText().equals("Run home.")));
        assertTrue(drafts.stream().anyMatch(draft -> draft.originalText().equals("Home")
                && draft.instanceText().equals("Home first.")));
        assertEquals(1, drafts.stream().filter(draft -> draft.originalText().equals("Run home.")
                && draft.instanceText() == null).count());
    }

    private DictionaryPhrase phrase(String source, String translation, String... tokens) {
        return new DictionaryPhrase(source,
                translation.isBlank() ? Set.of() : Set.of(translation),
                null,
                null,
                Set.of(tokens),
                source.toLowerCase(),
                false);
    }

    private PhraseExample example(String wordIdentity, DictionaryPhrase phrase) {
        return new PhraseExample(wordIdentity, phrase.sourceText(), phrase.translations(), phrase.movieTitle(), phrase.movieUrl());
    }
}
