package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewAttempt;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.model.WordExampleLink;
import com.puzzlemovies.export.repo.WordExampleLinkRepository;
import com.puzzlemovies.export.review.ReviewPresentation;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.puzzlemovies.export.repo.ReviewAttemptRepository;
import com.puzzlemovies.export.repo.ReviewCardRepository;
import com.puzzlemovies.export.review.BinaryReviewScheduler;
import com.puzzlemovies.export.review.ReviewScheduleResult;
import com.puzzlemovies.export.web.ReviewDtos;
import com.puzzlemovies.export.web.ReviewDtos.LookupAction;
import com.puzzlemovies.export.web.ReviewDtos.LookupActionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
    private final WordExampleLinkRepository links;
    private final LinkedVocabularyService vocabulary;
    private static final int PAGE_SIZE = 25;

    public ReviewService(ReviewCardRepository reviewCardRepository,
                         ReviewAttemptRepository reviewAttemptRepository,
                         BinaryReviewScheduler scheduler, WordExampleLinkRepository links,
                         LinkedVocabularyService vocabulary) {
        this.reviewCardRepository = reviewCardRepository;
        this.reviewAttemptRepository = reviewAttemptRepository;
        this.scheduler = scheduler;
        this.links = links;
        this.vocabulary = vocabulary;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Page<ReviewDtos.CardLibraryItem> cardLibrary(User user, int page) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be nonnegative");
        }
        return reviewCardRepository.findLibrary(user, PageRequest.of(page, 25))
                .map(row -> new ReviewDtos.CardLibraryItem(row.getId(), row.getVersion(), row.getOriginalText(),
                        row.getInstanceText(), row.getTranslationText(), row.getTotalAnswers(),
                        row.getCorrectAnswers(), row.getIncorrectAnswers(), row.getVocabularyEntryId(), row.getExampleCount()));
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
        vocabulary.lockOwner(user.getId());
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
        var entry = card.getVocabularyEntry();
        return new ReviewDtos.ReviewCardView(card.getId(), entry.getEffectiveOriginal(), null,
                entry.getEffectiveTranslation(), null, card.getState(), card.getDueAt(), lookupActions(card),
                entry.getVersion(), entry.getId(), links.countByUser_IdAndEntry_Id(card.getUser().getId(), entry.getId()),
                null, entry.getContentRevision());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDtos.ReviewCardView beginPresentation(User user, ReviewDtos.ReviewCardView view, HttpSession session) {
        if (view == null) return null;
        var card = ownedCard(user, view.id());
        var fresh = toView(card);
        return fresh.withPresentation(ReviewPresentation.open(session, user.getId(), card.getId(), card.getVersion(),
                fresh.effectiveContentVersion(), Instant.now()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDtos.ExamplePage<ReviewDtos.ExampleItem> reviewExamples(User user, UUID cardId, UUID presentationId,
                                                                        int page, HttpSession session) {
        var card = ownedCard(user, cardId);
        presentation(user, card, presentationId, session);
        var result = examplePage(user, card, page);
        var items = new ArrayList<ReviewDtos.ExampleItem>();
        long position = (long) page * PAGE_SIZE;
        for (var link : result) items.add(example(link, ++position));
        return new ReviewDtos.ExamplePage<>(cardId, presentationId, card.getVocabularyEntry().getContentRevision(),
                page, PAGE_SIZE, result.getTotalElements(), items);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDtos.ExamplePage<ReviewDtos.EditableExampleItem> libraryExamples(User user, UUID cardId, int page) {
        var card = ownedCard(user, cardId);
        var result = examplePage(user, card, page);
        var items = new ArrayList<ReviewDtos.EditableExampleItem>();
        long position = (long) page * PAGE_SIZE;
        for (var link : result) items.add(editableExample(link, ++position));
        return new ReviewDtos.ExamplePage<>(cardId, null, card.getVocabularyEntry().getContentRevision(),
                page, PAGE_SIZE, result.getTotalElements(), items);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDtos.MainReveal reveal(User user, UUID cardId, UUID presentationId, HttpSession session) {
        var card = ownedCard(user, cardId);
        ReviewPresentation.reveal(session, presentationId, user.getId(), cardId, card.getVersion(),
                card.getVocabularyEntry().getContentRevision(), Instant.now());
        return new ReviewDtos.MainReveal(cardId, presentationId, true, card.getVocabularyEntry().getEffectiveTranslation());
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ReviewDtos.ExampleReveal revealExample(User user, UUID cardId, UUID exampleId,
                                                   ReviewDtos.ExampleRevealRequest request, HttpSession session) {
        var card = ownedCard(user, cardId);
        var state = presentation(user, card, request.presentationId(), session);
        var link = ownedLink(user, card, exampleId);
        if (!state.mainRevealed()) throw new MainAnswerNotRevealed();
        if (request.linkVersion() == null || request.linkVersion() != link.getVersion()) throw new StaleCardException();
        String translation = link.getEffectiveTranslation();
        return new ReviewDtos.ExampleReveal(cardId, request.presentationId(), exampleId, link.getVersion(),
                translation, hasText(translation));
    }

    @Transactional
    public ReviewDtos.CardContentUpdateResponse updateContent(User user, UUID cardId,
                                                               ReviewDtos.CardContentUpdateRequest request) {
        vocabulary.lockOwner(user.getId());
        var card = ownedCard(user, cardId);
        var entry = card.getVocabularyEntry();
        requireVersion(request.version(), entry.getVersion());
        String original = requiredOriginal(request.originalText(), 1000);
        String translation = optionalText(request.translationText(), 8000, "Translation");
        boolean changed = !original.equals(entry.getEffectiveOriginal());
        entry.setOriginalOverride(original);
        entry.setTranslationOverride(translation);
        if (changed) vocabulary.rematch(user.getId());
        reviewCardRepository.flush();
        return new ReviewDtos.CardContentUpdateResponse(toView(card));
    }

    @Transactional
    public ReviewDtos.ExampleUpdateResponse updateExample(User user, UUID cardId, UUID exampleId,
                                                           ReviewDtos.ExampleUpdateRequest request) {
        vocabulary.lockOwner(user.getId());
        var card = ownedCard(user, cardId);
        var link = ownedLink(user, card, exampleId);
        requireVersion(request.version(), link.getVersion());
        String original = requiredOriginal(request.originalText(), 4000);
        String translation = optionalText(request.translationText(), 8000, "Translation");
        link.setOriginalOverride(original);
        link.setTranslationOverride(translation);
        var entry = card.getVocabularyEntry();
        entry.touchContent();
        links.flush();
        var ordered = links.findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(user.getId(), entry.getId());
        long position = ordered.indexOf(link) + 1L;
        return new ReviewDtos.ExampleUpdateResponse(editableExample(link, position), cardId, entry.getContentRevision());
    }

    private ReviewCard ownedCard(User user, UUID id) {
        return reviewCardRepository.findByIdAndUser(id, user).orElseThrow(CardNotFoundException::new);
    }

    private WordExampleLink ownedLink(User user, ReviewCard card, UUID example) {
        return links.findByUser_IdAndEntry_IdAndExample_Id(user.getId(), card.getVocabularyEntry().getId(), example)
                .orElseThrow(CardNotFoundException::new);
    }

    private ReviewPresentation presentation(User user, ReviewCard card, UUID id, HttpSession session) {
        return ReviewPresentation.require(session, id, user.getId(), card.getId(), card.getVersion(),
                card.getVocabularyEntry().getContentRevision(), Instant.now());
    }

    private Page<WordExampleLink> examplePage(User user, ReviewCard card, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid example page");
        var result = links.findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(
                user.getId(), card.getVocabularyEntry().getId(), PageRequest.of(page, PAGE_SIZE));
        if (page > 0 && result.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid example page");
        return result;
    }

    private ReviewDtos.ExampleItem example(WordExampleLink link, long position) {
        var source = link.getExample();
        return new ReviewDtos.ExampleItem(source.getId(), link.getId(), link.getVersion(), link.getEffectiveOriginal(),
                source.getMovieTitle(), movieUrl(source.getMovieUrl()), position);
    }

    private ReviewDtos.EditableExampleItem editableExample(WordExampleLink link, long position) {
        var item = example(link, position);
        return new ReviewDtos.EditableExampleItem(item.exampleId(), item.linkId(), item.linkVersion(), item.originalText(),
                item.movieTitle(), item.movieUrl(), position, link.getEffectiveTranslation());
    }

    private String movieUrl(String url) {
        if (!hasText(url)) return null;
        try {
            var uri = java.net.URI.create(url);
            return uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    ? uri.toASCIIString() : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void requireVersion(Long supplied, long actual) {
        if (supplied == null || supplied < 0) throw new InvalidCardContentException("A valid version is required.");
        if (supplied != actual) throw new StaleCardException();
    }

    private String requiredOriginal(String input, int limit) {
        String original = input == null ? "" : input.strip();
        if (original.isBlank() || original.length() > limit) {
            throw new InvalidCardContentException("Original text is required and must be at most " + limit + " characters.");
        }
        return original;
    }

    public static class MainAnswerNotRevealed extends RuntimeException {}

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
        if (hasText(card.getVocabularyEntry().getEffectiveOriginal())) {
            actions.add(new LookupAction(
                    LookupActionType.DICTIONARY,
                    "Dictionary",
                    googleTranslateUrl(card.getVocabularyEntry().getEffectiveOriginal()),
                    card.getVocabularyEntry().getEffectiveOriginal()));
            actions.add(new LookupAction(
                    LookupActionType.PRONUNCIATION,
                    "Pronunciation",
                    pronunciationUrl(card.getVocabularyEntry().getEffectiveOriginal()),
                    card.getVocabularyEntry().getEffectiveOriginal()));
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
