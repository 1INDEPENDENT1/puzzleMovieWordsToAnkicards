package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewCardRepository extends JpaRepository<ReviewCard, UUID> {
    Optional<ReviewCard> findByIdAndUser(UUID id, User user);

    Optional<ReviewCard> findByUserAndContentKey(User user, String contentKey);

    @Query("""
            select c from ReviewCard c
            where c.user = :user
              and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
              and c.dueAt <= :now
            order by c.dueAt asc, c.createdAt asc, c.id asc
            """)
    List<ReviewCard> findDueCards(@Param("user") User user, @Param("now") Instant now, Pageable pageable);

    @Query("""
            select count(c) from ReviewCard c
            where c.user = :user
              and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
              and c.dueAt <= :now
            """)
    long countDueCards(@Param("user") User user, @Param("now") Instant now);

    @Query("""
            select min(c.dueAt) from ReviewCard c
            where c.user = :user
              and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
              and c.dueAt > :now
            """)
    Optional<Instant> findNextDueAt(@Param("user") User user, @Param("now") Instant now);
}
