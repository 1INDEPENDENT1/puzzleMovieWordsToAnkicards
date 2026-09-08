package com.puzzlemovies.export.service;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.review.ReviewCardDraft;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewCardImportServiceTest {
    @Test
    void refreshPreservesCustomizedContentIdentityAndSchedule() {
        User user = ReviewTestFixtures.user("learner@example.com");
        Map<String, ReviewCard> cards = new HashMap<>();
        ReviewCardImportService service = new ReviewCardImportService(repositoryBackedBy(cards));
        ReviewCardDraft source = draft("Run", "Run home.", "бежать", "Arrival");
        service.importDrafts(user, java.util.List.of(source));
        ReviewCard card = cards.values().iterator().next();
        var id = card.getId();
        var due = card.getDueAt();
        card.setReviewCount(5);
        card.setManualContentOverride(true);
        card.setOriginalText("Sprint");
        card.setInstanceText("");
        card.setTranslationText("custom");
        service.importDrafts(user, java.util.List.of(source));
        assertEquals("Sprint", card.getOriginalText());
        assertEquals("", card.getInstanceText());
        assertEquals("custom", card.getTranslationText());
        assertEquals(id, card.getId());
        assertEquals(due, card.getDueAt());
        assertEquals(5, card.getReviewCount());
        assertEquals(service.contentKey(source), card.getContentKey());
    }

    @Test
    void refreshStillUpdatesUncustomizedContent() {
        User user = ReviewTestFixtures.user("learner@example.com");
        Map<String, ReviewCard> cards = new HashMap<>();
        ReviewCardImportService service = new ReviewCardImportService(repositoryBackedBy(cards));
        service.importDrafts(user, java.util.List.of(draft("run", null, "translation", null)));
        service.importDrafts(user, java.util.List.of(draft("RUN", null, "TRANSLATION", null)));
        assertEquals(1, cards.size());
        assertEquals("RUN", cards.values().iterator().next().getOriginalText());
        assertEquals("TRANSLATION", cards.values().iterator().next().getTranslationText());
    }

    @Test
    void generatesStableContentKeysAndPreventsDuplicateCards() {
        User user = ReviewTestFixtures.user("learner@example.com");
        Map<String, ReviewCard> cards = new HashMap<>();
        ReviewCardRepository repository = repositoryBackedBy(cards);
        ReviewCardImportService service = new ReviewCardImportService(repository);
        ReviewCardDraft draft = draft("Run", "I am running home.", "бежать", "Arrival");

        ReviewCardImportService.ImportResult first = service.importDrafts(user, java.util.List.of(draft));
        ReviewCardImportService.ImportResult second = service.importDrafts(user, java.util.List.of(draft));

        assertEquals(1, first.created());
        assertEquals(0, first.updated());
        assertEquals(0, second.created());
        assertEquals(1, second.updated());
        assertEquals(1, cards.size());
        assertEquals(service.contentKey(draft), cards.values().iterator().next().getContentKey());
    }

    @Test
    void keepsSeparateCardsForSameOriginalTextWithDifferentInstances() {
        ReviewCardImportService service = new ReviewCardImportService(mock(ReviewCardRepository.class));

        String first = service.contentKey(draft("Run", "Run home.", "бежать", null));
        String second = service.contentKey(draft("Run", "Run away.", "бежать", null));

        assertNotEquals(first, second);
    }

    @Test
    void preservesBlankTranslationsDuringImport() {
        User user = ReviewTestFixtures.user("learner@example.com");
        Map<String, ReviewCard> cards = new HashMap<>();
        ReviewCardRepository repository = repositoryBackedBy(cards);
        ReviewCardImportService service = new ReviewCardImportService(repository);

        service.importDrafts(user, java.util.List.of(draft("empty translation", null, "", null)));

        ReviewCard card = cards.values().iterator().next();
        assertTrue(card.getTranslationText().isEmpty());
    }

    @Test
    void mergesIdenticalStandaloneOverflowExamplesOnRepeatImport() {
        User user = ReviewTestFixtures.user("learner@example.com");
        Map<String, ReviewCard> cards = new HashMap<>();
        ReviewCardImportService service = new ReviewCardImportService(repositoryBackedBy(cards));
        ReviewCardDraft standalone = new ReviewCardDraft(
                "Runs daily.", null, "Бегает каждый день.", null, ExportRecord.RecordKind.PHRASE);

        service.importDrafts(user, java.util.List.of(standalone, standalone));

        assertEquals(1, cards.size());
    }

    private ReviewCardDraft draft(String original, String instance, String translation, String source) {
        return new ReviewCardDraft(original, instance, translation, source, ExportRecord.RecordKind.WORD);
    }

    private ReviewCardRepository repositoryBackedBy(Map<String, ReviewCard> cards) {
        ReviewCardRepository repository = mock(ReviewCardRepository.class);
        when(repository.findByUserAndContentKey(any(User.class), any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(cards.get(invocation.getArgument(1))));
        when(repository.save(any(ReviewCard.class))).thenAnswer(invocation -> {
            ReviewCard card = invocation.getArgument(0);
            if (card.getId() == null) {
                card.setId(java.util.UUID.randomUUID());
            }
            cards.put(card.getContentKey(), card);
            return card;
        });
        return repository;
    }
}
