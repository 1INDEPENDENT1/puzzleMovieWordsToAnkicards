package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.PuzzleSessionToken;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesAuthClient;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesAuthResult;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class AuthService {
    private final PuzzleMoviesAuthClient authClient;
    private final UserRepository userRepository;
    private final PuzzleSessionTokenRepository tokenRepository;

    public AuthService(PuzzleMoviesAuthClient authClient,
                       UserRepository userRepository,
                       PuzzleSessionTokenRepository tokenRepository) {
        this.authClient = authClient;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public AuthenticationResult authenticate(String email, String password) throws IOException, InterruptedException {
        PuzzleMoviesAuthResult authResult = authClient.authenticate(email, password);
        if (!authResult.isSuccess()) {
            return AuthenticationResult.failure(authResult.errorMessage()
                    .orElse("Authentication failed. Please try again."));
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User created = new User();
                    created.setEmail(email);
                    return created;
                });
        user = userRepository.save(user);

        PuzzleSessionToken token = tokenRepository.findByUser(user)
                .orElseGet(PuzzleSessionToken::new);
        token.setUser(user);
        token.setCookieHeader(authResult.cookieHeader().orElseThrow());
        tokenRepository.save(token);

        return AuthenticationResult.success(user);
    }

    public record AuthenticationResult(User user, String errorMessage) {
        public static AuthenticationResult success(User user) {
            return new AuthenticationResult(user, null);
        }

        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(null, errorMessage);
        }

        public boolean isSuccess() {
            return user != null;
        }
    }
}
