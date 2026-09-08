package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewAttempt;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.ReviewAttemptRepository;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.review.BinaryReviewScheduler;
import com.puzzlemovies.export.review.ReviewScheduleResult;
import com.puzzlemovies.export.web.ReviewDtos;
import com.puzzlemovies.export.web.ReviewDtos.LookupAction;
import com.puzzlemovies.export.web.ReviewDtos.LookupActionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewCardRepository reviewCardRepository;
    private final ReviewAttemptRepository reviewAttemptRepository;
    private final BinaryReviewScheduler scheduler;

    public ReviewService(ReviewCardRepository reviewCardRepository,
                         ReviewAttemptRepository reviewAttemptRepository,
                         BinaryReviewScheduler scheduler) {
        this.reviewCardRepository = reviewCardRepository;
        this.reviewAttemptRepository = reviewAttemptRepository;
        this.scheduler = scheduler;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.CardLibraryItem> cardLibrary(User user, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be nonnegative");
        }
        return reviewCardRepository.findLibrary(user, PageRequest.of(page, 25))
                .map(row -> new ReviewDtos.CardLibraryItem(row.getId(), row.getVersion(), row.getOriginalText(),
                        row.getInstanceText(), row.getTranslationText(), row.getTotalAnswers(),
                        row.getCorrectAnswers(), row.getIncorrectAnswers()));
    }

    @Transactional(readOnly = true)
    public ReviewDtos.ReviewQueueResponse nextDueCard(User user, int reviewedCount) {
        Instant now = Instant.now();
        List<ReviewCard> dueCards = reviewCardRepository.findDueCards(user, now, PageRequest.of(0, 1));
        ReviewDtos.ReviewCardView card = dueCards.isEmpty() ? null : toView(dueCards.get(0));
        return new ReviewDtos.ReviewQueueResponse(card, counts(user, reviewedCount, now));
    }

    @Transactional
    public ReviewDtos.ReviewAnswerResponse answerCard(User user,
                                                      UUID cardId,
                                                      ReviewAnswer answer,
                                                      Long responseMillis,
                                                      int reviewedCount) {
        if (answer == null) {
            throw new InvalidAnswerException();
        }
        Instant now = Instant.now();
        ReviewCard card = reviewCardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(CardNotFoundException::new);
        if (card.getState() == ReviewCardState.SUSPENDED || card.getDueAt().isAfter(now.plusSeconds(5))) {
            throw new CardNotAnswerableException();
        }

        ReviewScheduleResult schedule = scheduler.schedule(card, answer, now);
        ReviewAttempt attempt = new ReviewAttempt();
        attempt.setReviewCard(card);
        attempt.setUser(user);
        attempt.setAnswer(answer);
        attempt.setReviewedAt(now);
        attempt.setPreviousState(schedule.previousState());
        attempt.setNextState(schedule.nextState());
        attempt.setPreviousDueAt(schedule.previousDueAt());
        attempt.setNextDueAt(schedule.nextDueAt());
        attempt.setPreviousIntervalDays(schedule.previousIntervalDays());
        attempt.setNextIntervalDays(schedule.nextIntervalDays());
        attempt.setResponseMillis(responseMillis);

        card.setState(schedule.nextState());
        card.setDueAt(schedule.nextDueAt());
        card.setIntervalDays(schedule.nextIntervalDays());
        card.setEaseFactor(schedule.nextEaseFactor());
        card.setLapseCount(schedule.nextLapseCount());
        card.setReviewCount(card.getReviewCount() + 1);
        card.setLastReviewedAt(now);

        reviewAttemptRepository.save(attempt);
        reviewCardRepository.save(card);

        int nextReviewedCount = reviewedCount + 1;
        List<ReviewCard> dueCards = reviewCardRepository.findDueCards(user, now, PageRequest.of(0, 1));
        ReviewDtos.ReviewCardView nextCard = dueCards.isEmpty() ? null : toView(dueCards.get(0));
        ReviewDtos.SchedulingResult result = new ReviewDtos.SchedulingResult(
                card.getId(),
                answer,
                schedule.previousState(),
                schedule.nextState(),
                schedule.nextDueAt(),
                schedule.nextIntervalDays());
        return new ReviewDtos.ReviewAnswerResponse(result, nextCard, counts(user, nextReviewedCount, now));
    }

    public ReviewDtos.ReviewCardView toView(ReviewCard card) {
        return new ReviewDtos.ReviewCardView(
                card.getId(),
                card.getOriginalText(),
                card.getInstanceText(),
                card.getTranslationText(),
                card.getSourceContext(),
                card.getState(),
                card.getDueAt(),
                lookupActions(card),
                card.getVersion());
    }

    @Transactional
    public ReviewDtos.CardContentUpdateResponse updateContent(User user, UUID cardId,
                                                               ReviewDtos.CardContentUpdateRequest request) {
        ReviewCard card = reviewCardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(CardNotFoundException::new);
        if (request == null || request.version() == null || request.version() < 0) {
            throw new InvalidCardContentException("A valid card version is required.");
        }
        String original = request.originalText() == null ? "" : request.originalText().strip();
        if (original.isBlank() || original.length() > 1000) {
            throw new InvalidCardContentException("Original text is required and must be at most 1,000 characters.");
        }
        String instance = optionalText(request.instanceText(), 4000, "Example");
        String translation = optionalText(request.translationText(), 8000, "Translation");
        if (request.version() != card.getVersion()) {
            throw new StaleCardException();
        }
        card.setOriginalText(original);
        card.setInstanceText(instance);
        card.setTranslationText(translation);
        card.markContentCustomized();
        try {
            // Flush inside this transaction so racing edits return a conflict and no partial update.
            card = reviewCardRepository.saveAndFlush(card);
        } catch (OptimisticLockingFailureException ex) {
            throw new StaleCardException();
        }
        return new ReviewDtos.CardContentUpdateResponse(toView(card));
    }

    private String optionalText(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw new InvalidCardContentException(field + " must be at most " + maxLength + " characters.");
        }
        return value == null || value.isBlank() ? "" : value;
    }

    public static class InvalidCardContentException extends RuntimeException {
        public InvalidCardContentException(String message) {
            super(message);
        }
    }

    public static class StaleCardException extends RuntimeException {
    }

    private ReviewDtos.ReviewCounts counts(User user, int reviewedCount, Instant now) {
        return new ReviewDtos.ReviewCounts(
                Math.max(0, reviewedCount),
                reviewCardRepository.countDueCards(user, now),
                reviewCardRepository.findNextDueAt(user, now).orElse(null));
    }

    private List<LookupAction> lookupActions(ReviewCard card) {
        List<LookupAction> actions = new ArrayList<>();
        if (hasText(card.getOriginalText())) {
            actions.add(new LookupAction(
                    LookupActionType.DICTIONARY,
                    "Dictionary",
                    googleTranslateUrl(card.getOriginalText()),
                    card.getOriginalText()));
            actions.add(new LookupAction(
                    LookupActionType.PRONUNCIATION,
                    "Pronunciation",
                    pronunciationUrl(card.getOriginalText()),
                    card.getOriginalText()));
        }
        if (hasText(card.getInstanceText())) {
            actions.add(new LookupAction(
                    LookupActionType.INSTANCE_TRANSLATION,
                    "Translate sentence",
                    googleTranslateUrl(card.getInstanceText()),
                    card.getInstanceText()));
        }
        return actions;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String googleTranslateUrl(String text) {
        return UriComponentsBuilder.fromUriString("https://translate.google.com/")
                .queryParam("sl", "en")
                .queryParam("tl", "ru")
                .queryParam("text", text)
                .queryParam("op", "translate")
                .build()
                .encode()
                .toUriString();
    }

    private String pronunciationUrl(String text) {
        return UriComponentsBuilder.fromUriString("https://www.google.com/search")
                .queryParam("q", text + " pronunciation")
                .build()
                .encode()
                .toUriString();
    }

    public static class InvalidAnswerException extends RuntimeException {
    }

    public static class CardNotFoundException extends RuntimeException {
    }

    public static class CardNotAnswerableException extends RuntimeException {
    }
}
