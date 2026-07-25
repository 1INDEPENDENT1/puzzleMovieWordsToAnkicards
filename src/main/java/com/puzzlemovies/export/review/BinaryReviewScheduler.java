package com.puzzlemovies.export.review;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class BinaryReviewScheduler {
    static final Duration SHORT_RETRY = Duration.ofMinutes(10);
    static final double MIN_EASE = 1.3;
    static final double MAX_EASE = 3.0;

    public ReviewScheduleResult schedule(ReviewCard card, ReviewAnswer answer, Instant reviewedAt) {
        if (card.getState() == ReviewCardState.SUSPENDED) {
            throw new IllegalStateException("Suspended cards cannot be reviewed");
        }
        ReviewCardState previousState = card.getState();
        Instant previousDueAt = card.getDueAt();
        int previousInterval = card.getIntervalDays();
        double previousEase = card.getEaseFactor();
        int previousLapses = card.getLapseCount();

        ReviewCardState nextState;
        int nextInterval;
        double nextEase = previousEase;
        int nextLapses = previousLapses;
        Instant nextDueAt;

        if (answer == ReviewAnswer.KNOWN) {
            nextState = ReviewCardState.REVIEW;
            if (previousState == ReviewCardState.REVIEW) {
                nextInterval = growInterval(previousInterval, previousEase);
            } else if (previousState == ReviewCardState.RELEARNING && previousInterval > 1) {
                nextInterval = Math.max(1, (int) Math.ceil(previousInterval * 0.5));
            } else {
                nextInterval = 1;
            }
            nextDueAt = reviewedAt.plus(Duration.ofDays(nextInterval));
        } else {
            nextState = previousState == ReviewCardState.REVIEW || previousState == ReviewCardState.RELEARNING
                    ? ReviewCardState.RELEARNING
                    : ReviewCardState.LEARNING;
            nextInterval = 0;
            nextDueAt = reviewedAt.plus(SHORT_RETRY);
            if (previousState == ReviewCardState.REVIEW || previousState == ReviewCardState.RELEARNING) {
                nextLapses++;
            }
            nextEase = clamp(previousEase - 0.2);
        }

        return new ReviewScheduleResult(
                previousState,
                nextState,
                previousDueAt,
                nextDueAt,
                previousInterval,
                nextInterval,
                previousEase,
                nextEase,
                previousLapses,
                nextLapses);
    }

    private int growInterval(int previousInterval, double easeFactor) {
        if (previousInterval <= 0) {
            return 1;
        }
        int grown = (int) Math.ceil(previousInterval * easeFactor);
        return Math.max(previousInterval + 1, grown);
    }

    private double clamp(double easeFactor) {
        return Math.max(MIN_EASE, Math.min(MAX_EASE, easeFactor));
    }
}
