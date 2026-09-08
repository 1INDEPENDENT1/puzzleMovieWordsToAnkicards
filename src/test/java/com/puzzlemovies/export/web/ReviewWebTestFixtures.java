package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.User;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class ReviewWebTestFixtures {
    private ReviewWebTestFixtures() {
    }

    public static com.puzzlemovies.export.model.ReviewCard editableCard(User user, long version) {
        var card = com.puzzlemovies.export.review.ReviewTestFixtures.versionedCard(user, "Run", version);
        card.setInstanceText("I am running home.");
        card.setTranslationText("бежать");
        return card;
    }

    public static String contentUpdate(long version) {
        return "{\"originalText\":\"Walk\",\"instanceText\":\"Walk home.\","
                + "\"translationText\":\"идти\",\"version\":" + version + "}";
    }

    public static RequestPostProcessor signedIn(User user) {
        return request -> {
            request.getSession(true).setAttribute(SessionUserResolver.SESSION_USER_ID, user.getId().toString());
            return request;
        };
    }
}
