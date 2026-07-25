package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.User;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class ReviewWebTestFixtures {
    private ReviewWebTestFixtures() {
    }

    public static RequestPostProcessor signedIn(User user) {
        return request -> {
            request.getSession(true).setAttribute(SessionUserResolver.SESSION_USER_ID, user.getId().toString());
            return request;
        };
    }
}
