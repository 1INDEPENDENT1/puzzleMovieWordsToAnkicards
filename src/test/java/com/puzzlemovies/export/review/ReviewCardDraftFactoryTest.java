package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
