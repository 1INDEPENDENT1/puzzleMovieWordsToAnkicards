package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewCardRepository extends JpaRepository<ReviewCard, UUID> {
    interface CardLibraryRow {
        UUID getId();
        long getVersion();
        String getOriginalText();
        String getInstanceText();
        String getTranslationText();
        long getTotalAnswers();
        long getCorrectAnswers();
        long getIncorrectAnswers();
    }

    @Query(value = """
            select c.id as id, c.version as version, c.originalText as originalText,
                   c.instanceText as instanceText, c.translationText as translationText,
                   count(a.id) as totalAnswers,
                   sum(case when a.answer = com.puzzlemovies.export.model.ReviewAnswer.KNOWN then 1 else 0 end) as correctAnswers,
                   sum(case when a.answer = com.puzzlemovies.export.model.ReviewAnswer.UNKNOWN then 1 else 0 end) as incorrectAnswers
            from ReviewCard c left join ReviewAttempt a on a.reviewCard = c and a.user = :user
            where c.user = :user and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
            group by c.id, c.version, c.originalText, c.instanceText, c.translationText
            order by lower(c.originalText), c.id
            """, countQuery = """
            select count(c) from ReviewCard c
            where c.user = :user and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
            """)
    Page<CardLibraryRow> findLibrary(@Param("user") User user, Pageable pageable);

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
