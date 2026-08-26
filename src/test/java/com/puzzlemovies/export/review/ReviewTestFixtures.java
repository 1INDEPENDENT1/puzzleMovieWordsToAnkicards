package com.puzzlemovies.export.review;

import com.puzzlemovies.export.export.ExportRecord;
import com.puzzlemovies.export.export.CompleteWordExampleMatches;
import com.puzzlemovies.export.export.DictionaryPhrase;
import com.puzzlemovies.export.export.DictionaryWord;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewAttempt;
import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.User;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Map;

public final class ReviewTestFixtures {
    private ReviewTestFixtures() {
    }

    public static User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        return user;
    }

    public static ReviewCard dueCard(User user, String originalText) {
        ReviewCard card = new ReviewCard();
        card.setId(UUID.randomUUID());
        card.setUser(user);
        card.setOriginalText(originalText);
        card.setTranslationText("translation");
        card.setContentKey(UUID.randomUUID().toString());
        card.setRecordKind(ExportRecord.RecordKind.WORD);
        card.setState(ReviewCardState.NEW);
        card.setDueAt(Instant.now().minusSeconds(60));
        card.setEaseFactor(2.5);
        return card;
    }

    public static ReviewAttempt attempt(User user, ReviewCard card, ReviewAnswer answer) {
        ReviewAttempt attempt = new ReviewAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setUser(user);
        attempt.setReviewCard(card);
        attempt.setAnswer(answer);
        attempt.setReviewedAt(Instant.now());
        attempt.setPreviousState(ReviewCardState.NEW);
        attempt.setNextState(ReviewCardState.REVIEW);
        attempt.setPreviousDueAt(card.getDueAt());
        attempt.setNextDueAt(Instant.now().plusSeconds(86_400));
        attempt.setPreviousIntervalDays(0);
        attempt.setNextIntervalDays(1);
        return attempt;
    }

    public static ExportRecord wordRecordWithExample() {
        return new ExportRecord(
                "Run",
                "бежать<br><div class=\"example\">I am running home.<br><span class=\"translation\">Я бегу домой.</span><br><span class=\"movie\">Arrival</span></div>",
                ExportRecord.RecordKind.WORD);
    }

    public static ExportRecord phraseRecord() {
        return new ExportRecord("The moon is bright.", "Луна яркая.", ExportRecord.RecordKind.PHRASE);
    }

    public static ReviewExportSource source(List<DictionaryWord> words,
                                            List<DictionaryPhrase> examples,
                                            Map<String, List<com.puzzlemovies.export.export.PhraseExample>> matches) {
        return new ReviewExportSource(words, examples, new CompleteWordExampleMatches(matches));
    }
}
