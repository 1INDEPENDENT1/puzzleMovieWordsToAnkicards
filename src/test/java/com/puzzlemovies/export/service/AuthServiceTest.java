package com.puzzlemovies.export.service;

import com.puzzlemovies.export.model.PuzzleSessionToken;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesAuthClient;
import com.puzzlemovies.export.puzzlemovies.PuzzleMoviesAuthResult;
import com.puzzlemovies.export.repo.PuzzleSessionTokenRepository;
import com.puzzlemovies.export.repo.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    @Test
    void doesNotPersistUserOrTokenWhenAuthenticationFails() throws Exception {
        PuzzleMoviesAuthClient authClient = mock(PuzzleMoviesAuthClient.class);
        UserRepository userRepository = mock(UserRepository.class);
        PuzzleSessionTokenRepository tokenRepository = mock(PuzzleSessionTokenRepository.class);
        when(authClient.authenticate("user@example.com", "bad"))
                .thenReturn(PuzzleMoviesAuthResult.failure("Неверный пароль"));

        AuthService service = new AuthService(authClient, userRepository, tokenRepository);

        AuthService.AuthenticationResult result = service.authenticate("user@example.com", "bad");

        assertFalse(result.isSuccess());
        assertEquals("Неверный пароль", result.errorMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenRepository, never()).save(any(PuzzleSessionToken.class));
    }

    @Test
    void persistsUserAndTokenWhenAuthenticationSucceeds() throws Exception {
        PuzzleMoviesAuthClient authClient = mock(PuzzleMoviesAuthClient.class);
        UserRepository userRepository = mock(UserRepository.class);
        PuzzleSessionTokenRepository tokenRepository = mock(PuzzleSessionTokenRepository.class);
        when(authClient.authenticate("user@example.com", "good"))
                .thenReturn(PuzzleMoviesAuthResult.success("wp_logged_in_cookie=value"));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenRepository.findByUser(any(User.class))).thenReturn(Optional.empty());

        AuthService service = new AuthService(authClient, userRepository, tokenRepository);

        AuthService.AuthenticationResult result = service.authenticate("user@example.com", "good");

        assertTrue(result.isSuccess());
        assertEquals("user@example.com", result.user().getEmail());
        verify(userRepository).save(any(User.class));
        verify(tokenRepository).save(any(PuzzleSessionToken.class));
    }
}
