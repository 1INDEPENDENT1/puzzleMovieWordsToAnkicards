package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.model.VocabularyEntry;
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
        VocabularyEntry getEntry();
        default String getOriginalText() { return getEntry().getEffectiveOriginal(); }
        default String getInstanceText() { return null; }
        default String getTranslationText() { return getEntry().getEffectiveTranslation(); }
        default UUID getVocabularyEntryId() { return getEntry().getId(); }
        long getExampleCount();
        long getTotalAnswers();
        long getCorrectAnswers();
        long getIncorrectAnswers();
    }

    @Query(value = """
            select c.id as id, e.version as version, e as entry,
                   (select count(l.id) from WordExampleLink l where l.user = :user and l.entry = e) as exampleCount,
                   (select count(a.id) from ReviewAttempt a where a.reviewCard = c and a.user = :user) as totalAnswers,
                   (select count(a.id) from ReviewAttempt a where a.reviewCard = c and a.user = :user
                       and a.answer = com.puzzlemovies.export.model.ReviewAnswer.KNOWN) as correctAnswers,
                   (select count(a.id) from ReviewAttempt a where a.reviewCard = c and a.user = :user
                       and a.answer = com.puzzlemovies.export.model.ReviewAnswer.UNKNOWN) as incorrectAnswers
            from ReviewCard c join c.vocabularyEntry e
            where c.user = :user and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
            order by lower(coalesce(e.originalOverride, e.sourceOriginal)), c.id
            """, countQuery = """
            select count(c) from ReviewCard c
            where c.user = :user and c.state <> com.puzzlemovies.export.model.ReviewCardState.SUSPENDED
            """)
    Page<CardLibraryRow> findLibrary(@Param("user") User user, Pageable pageable);

    Optional<ReviewCard> findByIdAndUser(UUID id, User user);

    @Query("select c.vocabularyEntry.id from ReviewCard c where c.user.id = :userId")
    List<UUID> findEntryIdsByUserId(@Param("userId") UUID userId);

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
