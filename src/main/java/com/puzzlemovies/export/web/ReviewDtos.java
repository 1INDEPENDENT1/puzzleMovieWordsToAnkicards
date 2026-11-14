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
                                  long incorrectAnswers, UUID vocabularyEntryId, long exampleCount) {
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
                                 long version, UUID vocabularyEntryId, long exampleCount,
                                 UUID presentationId, long effectiveContentVersion) {
        public ReviewCardView withPresentation(UUID id) {
            return new ReviewCardView(this.id, originalText, instanceText, translationText, sourceContext, state, dueAt,
                    lookupActions, version, vocabularyEntryId, exampleCount, id, effectiveContentVersion);
        }
    }

    public record CardContentUpdateRequest(String originalText, String translationText, Long version) {
    }

    public record CardContentUpdateResponse(ReviewCardView card) {
    }

    public record ExampleItem(UUID exampleId, UUID linkId, long linkVersion, String originalText,
                              String movieTitle, String movieUrl, long position) {}

    public record EditableExampleItem(UUID exampleId, UUID linkId, long linkVersion, String originalText,
                                      String movieTitle, String movieUrl, long position, String translationText) {}

    public record ExamplePage<T>(UUID cardId, UUID presentationId, long effectiveContentVersion,
                                 int page, int pageSize, long total, List<T> items) {}

    public record PresentationRequest(UUID presentationId) {}
    public record MainReveal(UUID cardId, UUID presentationId, boolean mainRevealed, String translationText) {}
    public record ExampleRevealRequest(UUID presentationId, Long linkVersion) {}
    public record ExampleReveal(UUID cardId, UUID presentationId, UUID exampleId, long linkVersion,
                                String translationText, boolean translationAvailable) {}
    public record ExampleUpdateRequest(String originalText, String translationText, Long version) {}
    public record ExampleUpdateResponse(EditableExampleItem item, UUID cardId, long effectiveContentVersion) {}

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
