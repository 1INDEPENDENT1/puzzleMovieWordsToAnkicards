package com.puzzlemovies.export.review;

import jakarta.servlet.http.HttpSession;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ReviewPresentation(UUID ownerId, UUID cardId, long cardVersion, long contentVersion,
                                 boolean mainRevealed, Instant expiresAt) {
    private static final String ATTRIBUTE = ReviewPresentation.class.getName();
    private static final int LIMIT = 32;

    public static UUID open(HttpSession session, UUID owner, UUID card, long version, long revision, Instant now) {
        synchronized (session) {
            var states = states(session);
            states.values().removeIf(state -> !now.isBefore(state.expiresAt()));
            while (states.size() >= LIMIT) {
                var keys = states.keySet().iterator();
                keys.next();
                keys.remove();
            }
            UUID id = UUID.randomUUID();
            states.put(id, new ReviewPresentation(owner, card, version, revision, false, now.plusSeconds(1800)));
            return id;
        }
    }

    public static ReviewPresentation require(HttpSession session, UUID id, UUID owner, UUID card,
                                             long version, long revision, Instant now) {
        synchronized (session) {
            var state = states(session).get(id);
            if (state == null || !state.ownerId().equals(owner) || !state.cardId().equals(card)
                    || state.cardVersion() != version || state.contentVersion() != revision
                    || !now.isBefore(state.expiresAt())) {
                throw new Stale();
            }
            return state;
        }
    }

    public static void reveal(HttpSession session, UUID id, UUID owner, UUID card, long version,
                              long revision, Instant now) {
        synchronized (session) {
            var current = require(session, id, owner, card, version, revision, now);
            states(session).put(id, new ReviewPresentation(owner, card, version, revision, true, current.expiresAt()));
        }
    }

    public static void invalidate(HttpSession session, UUID owner, UUID card) {
        synchronized (session) {
            states(session).values().removeIf(state -> state.ownerId().equals(owner) && state.cardId().equals(card));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ReviewPresentation> states(HttpSession session) {
        var states = (Map<UUID, ReviewPresentation>) session.getAttribute(ATTRIBUTE);
        if (states == null) {
            states = java.util.Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true));
            session.setAttribute(ATTRIBUTE, states);
        }
        return states;
    }

    public static class Stale extends RuntimeException {}
}
