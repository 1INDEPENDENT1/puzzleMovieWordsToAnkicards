package com.puzzlemovies.export.review;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ReviewPresentationTest {
    @Test
    void revealBelongsToOneVersionOfOnePresentationInOneSession() {
        var session = new MockHttpSession();
        var owner = UUID.randomUUID();
        var card = UUID.randomUUID();
        var now = Instant.parse("2026-11-11T00:00:00Z");
        var first = ReviewPresentation.open(session, owner, card, 4, 7, now);
        var second = ReviewPresentation.open(session, owner, card, 4, 7, now);
        ReviewPresentation.reveal(session, first, owner, card, 4, 7, now);
        assertTrue(ReviewPresentation.require(session, first, owner, card, 4, 7, now).mainRevealed());
        assertFalse(ReviewPresentation.require(session, second, owner, card, 4, 7, now).mainRevealed());
        assertThrows(ReviewPresentation.Stale.class,
                () -> ReviewPresentation.require(session, first, owner, card, 4, 8, now));
        assertThrows(ReviewPresentation.Stale.class,
                () -> ReviewPresentation.require(new MockHttpSession(), first, owner, card, 4, 7, now));
        assertThrows(ReviewPresentation.Stale.class,
                () -> ReviewPresentation.require(session, first, owner, card, 4, 7, now.plusSeconds(1801)));
        ReviewPresentation.invalidate(session, owner, card);
        assertThrows(ReviewPresentation.Stale.class,
                () -> ReviewPresentation.require(session, second, owner, card, 4, 7, now));
    }
}
