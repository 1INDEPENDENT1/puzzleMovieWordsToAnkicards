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
