package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewCardState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReviewDtos {
    private ReviewDtos() {
    }

    public record CardLibraryItem(UUID id, long version, String originalText, String instanceText,
                                  String translationText, long totalAnswers, long correctAnswers,
                                  long incorrectAnswers) {
        public Double correctAnswerPercentage() {
            return totalAnswers == 0 ? null : 100.0 * correctAnswers / totalAnswers;
        }
    }

    public enum LookupActionType {
        DICTIONARY,
        PRONUNCIATION,
        INSTANCE_TRANSLATION
    }

    public record ReviewCardView(UUID id,
                                 String originalText,
                                 String instanceText,
                                 String translationText,
                                 String sourceContext,
                                 ReviewCardState state,
                                 Instant dueAt,
                                 List<LookupAction> lookupActions,
                                 long version) {
    }

    public record CardContentUpdateRequest(String originalText, String instanceText,
                                           String translationText, Long version) {
    }

    public record CardContentUpdateResponse(ReviewCardView card) {
    }

    public record LookupAction(LookupActionType type, String label, String url, String sourceText) {
    }

    public record ReviewCounts(int reviewedCount, long remainingDueCount, Instant nextDueAt) {
    }

    public record ReviewQueueResponse(ReviewCardView card, ReviewCounts counts) {
    }

    public record ReviewAnswerRequest(ReviewAnswer answer, Long responseMillis) {
    }

    public record SchedulingResult(UUID cardId,
                                   ReviewAnswer answer,
                                   ReviewCardState previousState,
                                   ReviewCardState nextState,
                                   Instant nextDueAt,
                                   int nextIntervalDays) {
    }

    public record ReviewAnswerResponse(SchedulingResult result,
                                       ReviewCardView nextCard,
                                       ReviewCounts counts) {
    }
}
