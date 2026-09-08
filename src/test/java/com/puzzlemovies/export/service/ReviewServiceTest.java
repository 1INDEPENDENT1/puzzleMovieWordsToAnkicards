package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewAttempt;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ReviewAttemptRepository;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.review.BinaryReviewScheduler;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import com.puzzlemovies.export.web.ReviewDtos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceTest {
    @Test
    void contentEditPreservesScheduleIdentityAndHistoryAndRefreshesLookups() {
        User user = ReviewTestFixtures.user("edit@example.com");
        var card = ReviewTestFixtures.versionedCard(user, "Run", 4);
        card.setState(ReviewCardState.REVIEW);
        card.setReviewCount(8);
        card.setLapseCount(2);
        card.setIntervalDays(7);
        card.setEaseFactor(2.3);
        card.setLastReviewedAt(Instant.now().minusSeconds(800));
        var due = card.getDueAt();
        var last = card.getLastReviewedAt();
        var key = card.getContentKey();
        var cards = mock(ReviewCardRepository.class);
        var attempts = mock(ReviewAttemptRepository.class);
        when(cards.findByIdAndUser(card.getId(), user)).thenReturn(Optional.of(card));
        when(cards.saveAndFlush(card)).thenAnswer(invocation -> {
            org.springframework.test.util.ReflectionTestUtils.setField(card, "version", 5L);
            return card;
        });
        var service = new ReviewService(cards, attempts, new BinaryReviewScheduler());
        var updated = service.updateContent(user, card.getId(),
                new ReviewDtos.CardContentUpdateRequest("  Walk  ", "Walk home.", "", 4L)).card();
        assertEquals("Walk", updated.originalText());
        assertEquals("", updated.translationText());
        assertEquals("Walk home.", updated.instanceText());
        assertEquals(5, updated.version());
        assertEquals("Walk", updated.lookupActions().get(0).sourceText());
        org.junit.jupiter.api.Assertions.assertTrue(card.isManualContentOverride());
        assertEquals(key, card.getContentKey());
        assertEquals(due, card.getDueAt());
        assertEquals(last, card.getLastReviewedAt());
        assertEquals(8, card.getReviewCount());
        assertEquals(2, card.getLapseCount());
        assertEquals(7, card.getIntervalDays());
        assertEquals(2.3, card.getEaseFactor());
        assertEquals(ReviewCardState.REVIEW, card.getState());
        org.mockito.Mockito.verifyNoInteractions(attempts);
    }

    @Test
    void invalidOrStaleEditsNeverMutateSavedContent() {
        User user = ReviewTestFixtures.user("edit@example.com");
        var card = ReviewTestFixtures.versionedCard(user, "Run", 4);
        var cards = mock(ReviewCardRepository.class);
        var attempts = mock(ReviewAttemptRepository.class);
        when(cards.findByIdAndUser(card.getId(), user)).thenReturn(Optional.of(card));
        var service = new ReviewService(cards, attempts, new BinaryReviewScheduler());
        var invalidRequests = List.of(
                new ReviewDtos.CardContentUpdateRequest(" \n ", "", "", 4L),
                new ReviewDtos.CardContentUpdateRequest(null, "", "", 4L),
                new ReviewDtos.CardContentUpdateRequest("a".repeat(1001), "", "", 4L),
                new ReviewDtos.CardContentUpdateRequest("valid", "a".repeat(4001), "", 4L),
                new ReviewDtos.CardContentUpdateRequest("valid", "", "a".repeat(8001), 4L),
                new ReviewDtos.CardContentUpdateRequest("valid", null, null, null),
                new ReviewDtos.CardContentUpdateRequest("valid", null, null, -1L));
        for (var request : invalidRequests) {
            assertThrows(ReviewService.InvalidCardContentException.class,
                    () -> service.updateContent(user, card.getId(), request));
        }
        assertThrows(ReviewService.StaleCardException.class, () -> service.updateContent(user, card.getId(),
                new ReviewDtos.CardContentUpdateRequest("Walk", null, null, 3L)));
        assertThrows(ReviewService.CardNotFoundException.class, () -> service.updateContent(user, java.util.UUID.randomUUID(),
                new ReviewDtos.CardContentUpdateRequest("Walk", null, null, 4L)));
        assertEquals("Run", card.getOriginalText());
        assertFalse(card.isManualContentOverride());
        org.mockito.Mockito.verify(cards, org.mockito.Mockito.never()).saveAndFlush(any());
        org.mockito.Mockito.verifyNoInteractions(attempts);
    }

    @Test
    void concurrentDatabaseUpdateBecomesStaleConflict() {
        var user = ReviewTestFixtures.user("edit@example.com");
        var card = ReviewTestFixtures.versionedCard(user, "Run", 0);
        var cards = mock(ReviewCardRepository.class);
        when(cards.findByIdAndUser(card.getId(), user)).thenReturn(Optional.of(card));
        when(cards.saveAndFlush(card)).thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(ReviewCard.class, card.getId()));
        var service = new ReviewService(cards, mock(ReviewAttemptRepository.class), new BinaryReviewScheduler());
        assertThrows(ReviewService.StaleCardException.class, () -> service.updateContent(user, card.getId(),
                new ReviewDtos.CardContentUpdateRequest("Walk", null, null, 0L)));
    }

    @Test
    void libraryUsesAttemptTotalsAndLeavesUnansweredPercentageUnavailable() {
        User user = ReviewTestFixtures.user("library@example.com");
        ReviewCardRepository cards = mock(ReviewCardRepository.class);
        var mixed = mock(ReviewCardRepository.CardLibraryRow.class);
        when(mixed.getTotalAnswers()).thenReturn(3L);
        when(mixed.getCorrectAnswers()).thenReturn(2L);
        when(mixed.getIncorrectAnswers()).thenReturn(1L);
        var empty = mock(ReviewCardRepository.CardLibraryRow.class);
        when(cards.findLibrary(eq(user), any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(mixed, empty)));
        var attempts = mock(ReviewAttemptRepository.class);
        var page = new ReviewService(cards, attempts, new BinaryReviewScheduler()).cardLibrary(user, 2);
        assertEquals(3, page.getContent().get(0).totalAnswers());
        assertEquals(2, page.getContent().get(0).correctAnswers());
        assertEquals(1, page.getContent().get(0).incorrectAnswers());
        assertEquals(200.0 / 3, page.getContent().get(0).correctAnswerPercentage(), 0.0001);
        assertEquals(null, page.getContent().get(1).correctAnswerPercentage());
        verify(cards).findLibrary(user, org.springframework.data.domain.PageRequest.of(2, 25));
        org.mockito.Mockito.verifyNoInteractions(attempts);
    }

    @Test
    void loadsDueQueueAndBuildsLookupActionsWithoutPersistedUrls() {
        User user = ReviewTestFixtures.user("learner@example.com");
        ReviewCard card = ReviewTestFixtures.dueCard(user, "Run");
        card.setInstanceText("I am running home.");
        ReviewCardRepository cards = mock(ReviewCardRepository.class);
        when(cards.findDueCards(eq(user), any(Instant.class), any(Pageable.class))).thenReturn(List.of(card));
        when(cards.countDueCards(eq(user), any(Instant.class))).thenReturn(1L);
        when(cards.findNextDueAt(eq(user), any(Instant.class))).thenReturn(Optional.empty());
        ReviewService service = new ReviewService(cards, mock(ReviewAttemptRepository.class), new BinaryReviewScheduler());

        ReviewDtos.ReviewQueueResponse queue = service.nextDueCard(user, 2);

        assertEquals("Run", queue.card().originalText());
        assertEquals(2, queue.counts().reviewedCount());
        assertEquals(1, queue.counts().remainingDueCount());
        assertEquals(3, queue.card().lookupActions().size());
        assertFalse(queue.card().lookupActions().get(0).url().isBlank());
    }

    @Test
    void returnsEmptyStateCountsWhenNoCardsAreDue() {
        User user = ReviewTestFixtures.user("learner@example.com");
        ReviewCardRepository cards = mock(ReviewCardRepository.class);
        Instant next = Instant.parse("2026-07-17T10:00:00Z");
        when(cards.findDueCards(eq(user), any(Instant.class), any(Pageable.class))).thenReturn(List.of());
        when(cards.countDueCards(eq(user), any(Instant.class))).thenReturn(0L);
        when(cards.findNextDueAt(eq(user), any(Instant.class))).thenReturn(Optional.of(next));
        ReviewService service = new ReviewService(cards, mock(ReviewAttemptRepository.class), new BinaryReviewScheduler());

        ReviewDtos.ReviewQueueResponse queue = service.nextDueCard(user, 0);

        assertEquals(null, queue.card());
        assertEquals(next, queue.counts().nextDueAt());
    }

    @Test
    void answerPersistsAttemptWithPreviousAndNextSchedulingState() {
        User user = ReviewTestFixtures.user("learner@example.com");
        ReviewCard card = ReviewTestFixtures.dueCard(user, "Run");
        ReviewCardRepository cards = mock(ReviewCardRepository.class);
        ReviewAttemptRepository attempts = mock(ReviewAttemptRepository.class);
        when(cards.findByIdAndUser(card.getId(), user)).thenReturn(Optional.of(card));
        when(cards.findDueCards(eq(user), any(Instant.class), any(Pageable.class))).thenReturn(List.of());
        when(cards.countDueCards(eq(user), any(Instant.class))).thenReturn(0L);
        when(cards.findNextDueAt(eq(user), any(Instant.class))).thenReturn(Optional.empty());
        ReviewService service = new ReviewService(cards, attempts, new BinaryReviewScheduler());

        ReviewDtos.ReviewAnswerResponse response = service.answerCard(user, card.getId(), ReviewAnswer.KNOWN, 1200L, 0);

        assertEquals(ReviewCardState.REVIEW, card.getState());
        assertEquals(1, card.getReviewCount());
        assertNotNull(card.getLastReviewedAt());
        assertEquals(1, response.counts().reviewedCount());
        ArgumentCaptor<ReviewAttempt> captor = ArgumentCaptor.forClass(ReviewAttempt.class);
        verify(attempts).save(captor.capture());
        assertEquals(ReviewCardState.NEW, captor.getValue().getPreviousState());
        assertEquals(ReviewCardState.REVIEW, captor.getValue().getNextState());
        assertEquals(1200L, captor.getValue().getResponseMillis());
    }

    @Test
    void rejectsFutureOrMissingCards() {
        User user = ReviewTestFixtures.user("learner@example.com");
        ReviewCard future = ReviewTestFixtures.dueCard(user, "Run");
        future.setDueAt(Instant.now().plusSeconds(3600));
        ReviewCardRepository cards = mock(ReviewCardRepository.class);
        when(cards.findByIdAndUser(future.getId(), user)).thenReturn(Optional.of(future));
        ReviewService service = new ReviewService(cards, mock(ReviewAttemptRepository.class), new BinaryReviewScheduler());

        assertThrows(ReviewService.CardNotAnswerableException.class,
                () -> service.answerCard(user, future.getId(), ReviewAnswer.KNOWN, null, 0));
        assertThrows(ReviewService.CardNotFoundException.class,
                () -> service.answerCard(user, java.util.UUID.randomUUID(), ReviewAnswer.KNOWN, null, 0));
    }
}
