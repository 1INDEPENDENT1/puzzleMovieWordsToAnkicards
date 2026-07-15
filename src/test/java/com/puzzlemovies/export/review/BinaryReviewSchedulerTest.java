package com.puzzlemovies.export.review;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinaryReviewSchedulerTest {
    private final BinaryReviewScheduler scheduler = new BinaryReviewScheduler();
    private final User user = ReviewTestFixtures.user("learner@example.com");
    private final Instant now = Instant.parse("2026-07-16T10:00:00Z");

    @Test
    void knownGraduatesNewLearningAndRelearningCardsToReviewTomorrow() {
        assertGraduates(ReviewCardState.NEW);
        assertGraduates(ReviewCardState.LEARNING);
        assertGraduates(ReviewCardState.RELEARNING);
    }

    @Test
    void unknownSchedulesShortRetryAndMovesReviewCardsToRelearning() {
        ReviewCard card = card(ReviewCardState.REVIEW, 4, 2.5);

        ReviewScheduleResult result = scheduler.schedule(card, ReviewAnswer.UNKNOWN, now);

        assertEquals(ReviewCardState.RELEARNING, result.nextState());
        assertEquals(0, result.nextIntervalDays());
        assertEquals(now.plus(Duration.ofMinutes(10)), result.nextDueAt());
        assertEquals(1, result.nextLapseCount());
        assertTrue(result.nextEaseFactor() < result.previousEaseFactor());
    }

    @Test
    void repeatedKnownReviewAnswersGrowIntervalByAtLeastOneDay() {
        ReviewCard card = card(ReviewCardState.REVIEW, 3, 2.0);

        ReviewScheduleResult result = scheduler.schedule(card, ReviewAnswer.KNOWN, now);

        assertEquals(ReviewCardState.REVIEW, result.nextState());
        assertEquals(6, result.nextIntervalDays());
        assertEquals(now.plus(Duration.ofDays(6)), result.nextDueAt());
    }

    @Test
    void repeatedUnknownLearningAnswersStayInShortRetry() {
        ReviewCard card = card(ReviewCardState.LEARNING, 0, 1.3);

        ReviewScheduleResult result = scheduler.schedule(card, ReviewAnswer.UNKNOWN, now);

        assertEquals(ReviewCardState.LEARNING, result.nextState());
        assertEquals(now.plus(Duration.ofMinutes(10)), result.nextDueAt());
        assertEquals(0, result.nextIntervalDays());
        assertEquals(0, result.nextLapseCount());
        assertEquals(1.3, result.nextEaseFactor());
    }

    @Test
    void repeatedUnknownRelearningAnswersStayInRelearningAndCountLapses() {
        ReviewCard card = card(ReviewCardState.RELEARNING, 0, 2.0);

        ReviewScheduleResult result = scheduler.schedule(card, ReviewAnswer.UNKNOWN, now);

        assertEquals(ReviewCardState.RELEARNING, result.nextState());
        assertEquals(now.plus(Duration.ofMinutes(10)), result.nextDueAt());
        assertEquals(1, result.nextLapseCount());
    }

    private void assertGraduates(ReviewCardState state) {
        ReviewCard card = card(state, 0, 2.5);

        ReviewScheduleResult result = scheduler.schedule(card, ReviewAnswer.KNOWN, now);

        assertEquals(ReviewCardState.REVIEW, result.nextState());
        assertEquals(1, result.nextIntervalDays());
        assertEquals(now.plus(Duration.ofDays(1)), result.nextDueAt());
    }

    private ReviewCard card(ReviewCardState state, int intervalDays, double ease) {
        ReviewCard card = ReviewTestFixtures.dueCard(user, "Run");
        card.setState(state);
        card.setIntervalDays(intervalDays);
        card.setEaseFactor(ease);
        card.setDueAt(now.minusSeconds(30));
        return card;
    }
}
