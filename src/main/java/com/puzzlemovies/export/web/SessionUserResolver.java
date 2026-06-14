package com.puzzlemovies.export.web;

import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SessionUserResolver {
    public static final String SESSION_USER_ID = "puzzlemoviesUserId";

    private final UserRepository userRepository;
    private final PuzzleSessionTokenRepository tokenRepository;

    public SessionUserResolver(UserRepository userRepository, PuzzleSessionTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    public Optional<User> resolve(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_ID);
        if (!(value instanceof String idString)) {
            return Optional.empty();
        }
        try {
            UUID userId = UUID.fromString(idString);
            return userRepository.findById(userId)
                    .filter(user -> tokenRepository.findByUser(user).isPresent());
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public void bind(HttpSession session, User user) {
        session.setAttribute(SESSION_USER_ID, user.getId().toString());
    }

    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_USER_ID);
    }
}
