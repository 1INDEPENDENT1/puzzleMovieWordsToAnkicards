package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.PuzzleSessionToken;
import com.puzzlemovies.export.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PuzzleSessionTokenRepository extends JpaRepository<PuzzleSessionToken, UUID> {
    Optional<PuzzleSessionToken> findByUser(User user);
}
