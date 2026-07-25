package com.puzzlemovies.export.review;

import com.puzzlemovies.export.model.ReviewCardState;

import java.time.Instant;

public record ReviewScheduleResult(ReviewCardState previousState,
                                   ReviewCardState nextState,
                                   Instant previousDueAt,
                                   Instant nextDueAt,
                                   int previousIntervalDays,
                                   int nextIntervalDays,
                                   double previousEaseFactor,
                                   double nextEaseFactor,
                                   int previousLapseCount,
                                   int nextLapseCount) {
}
