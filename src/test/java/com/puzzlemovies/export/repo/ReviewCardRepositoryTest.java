package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import com.puzzlemovies.export.model.ReviewAnswer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.domain.PageRequest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("postgresAvailable")
class ReviewCardRepositoryTest {
    static PostgreSQLContainer<?> postgres;
    @Autowired ReviewCardRepository cards;
    @Autowired ReviewAttemptRepository attempts;
    @Autowired EntityManager em;

    static boolean postgresAvailable() {
        return System.getProperty("review.test.jdbc-url") != null
                || org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (System.getProperty("review.test.jdbc-url") != null) {
            registry.add("spring.datasource.url", () -> System.getProperty("review.test.jdbc-url"));
            registry.add("spring.datasource.username", () -> System.getProperty("review.test.username", "postgres"));
            registry.add("spring.datasource.password", () -> System.getProperty("review.test.password", ""));
        } else {
            postgres = new PostgreSQLContainer<>("postgres:17-alpine");
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Test
    void aggregatesActiveOwnedCardsIncludingZeroHistoryWithStablePageBoundaries() {
        var owner = ReviewTestFixtures.user("owner@example.com");
        var other = ReviewTestFixtures.user("other@example.com");
        em.persist(owner);
        em.persist(other);
        var zero = ReviewTestFixtures.dueCard(owner, "000 empty");
        zero.getVocabularyEntry().setTranslationOverride("");
        zero.setReviewCount(99); // Cached scheduling counts must not supply library statistics.
        persistCard(zero);
        var mixed = ReviewTestFixtures.dueCard(owner, "001 mixed");
        persistCard(mixed);
        ReviewTestFixtures.history(owner, mixed, ReviewAnswer.KNOWN, ReviewAnswer.UNKNOWN, ReviewAnswer.KNOWN)
                .forEach(em::persist);
        var known = ReviewTestFixtures.dueCard(owner, "002 known");
        persistCard(known);
        em.persist(ReviewTestFixtures.attempt(owner, known, ReviewAnswer.KNOWN));
        persistCard(ReviewTestFixtures.suspendedCard(owner, "hidden"));
        persistCard(ReviewTestFixtures.dueCard(other, "foreign"));
        for (int i = 0; i < 25; i++) persistCard(ReviewTestFixtures.dueCard(owner, "same"));
        em.flush();
        em.clear();
        var first = cards.findLibrary(owner, PageRequest.of(0, 25));
        var second = cards.findLibrary(owner, PageRequest.of(1, 25));
        assertEquals(28, first.getTotalElements());
        assertEquals(25, first.getNumberOfElements());
        assertEquals(3, second.getNumberOfElements());
        assertEquals(0, first.getContent().get(0).getTotalAnswers());
        assertEquals("", first.getContent().get(0).getTranslationText());
        var stats = first.getContent().get(1);
        assertEquals(3, stats.getTotalAnswers());
        assertEquals(2, stats.getCorrectAnswers());
        assertEquals(1, stats.getIncorrectAnswers());
        assertEquals(1, first.getContent().get(2).getCorrectAnswers());
        var ids = java.util.stream.Stream.concat(first.stream(), second.stream())
                .map(ReviewCardRepository.CardLibraryRow::getId).toList();
        assertEquals(28, new java.util.HashSet<>(ids).size());
        assertEquals(first.map(ReviewCardRepository.CardLibraryRow::getId).getContent(),
                cards.findLibrary(owner, PageRequest.of(0, 25)).map(ReviewCardRepository.CardLibraryRow::getId).getContent());
        assertEquals(1, cards.findLibrary(other, PageRequest.of(0, 25)).getTotalElements());
        assertTrue(cards.findLibrary(owner, PageRequest.of(2, 25)).isEmpty());
    }

    @Test
    void databaseRejectsAnEntityLoadedBeforeAnotherUpdate() {
        var user = ReviewTestFixtures.user("race@example.com");
        em.persist(user);
        var stale = ReviewTestFixtures.dueCard(user, "Run");
        persistCard(stale);
        em.flush();
        em.detach(stale);
        var current = em.find(ReviewCard.class, stale.getId());
        current.setReviewCount(1);
        em.flush();
        em.clear();
        stale.setReviewCount(2);
        assertThrows(jakarta.persistence.OptimisticLockException.class, () -> {
            em.merge(stale);
            em.flush();
        });
    }

    private void persistCard(ReviewCard card) {
        em.persist(card.getVocabularyEntry());
        em.persist(card);
    }
}
