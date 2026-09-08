package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.model.ReviewCard;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        zero.setTranslationText(null);
        zero.setReviewCount(99); // Cached scheduling counts must not supply library statistics.
        em.persist(zero);
        var mixed = ReviewTestFixtures.dueCard(owner, "001 mixed");
        em.persist(mixed);
        ReviewTestFixtures.history(owner, mixed, ReviewAnswer.KNOWN, ReviewAnswer.UNKNOWN, ReviewAnswer.KNOWN)
                .forEach(em::persist);
        var known = ReviewTestFixtures.dueCard(owner, "002 known");
        em.persist(known);
        em.persist(ReviewTestFixtures.attempt(owner, known, ReviewAnswer.KNOWN));
        em.persist(ReviewTestFixtures.suspendedCard(owner, "hidden"));
        em.persist(ReviewTestFixtures.dueCard(other, "foreign"));
        for (int i = 0; i < 25; i++) em.persist(ReviewTestFixtures.dueCard(owner, "same"));
        em.flush();
        em.clear();
        var first = cards.findLibrary(owner, PageRequest.of(0, 25));
        var second = cards.findLibrary(owner, PageRequest.of(1, 25));
        assertEquals(28, first.getTotalElements());
        assertEquals(25, first.getNumberOfElements());
        assertEquals(3, second.getNumberOfElements());
        assertEquals(0, first.getContent().get(0).getTotalAnswers());
        assertNull(first.getContent().get(0).getTranslationText());
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
    void persistedContentEditIncrementsVersionAndPreservesAttemptAndSchedule() {
        var user = ReviewTestFixtures.user("edit-db@example.com");
        em.persist(user);
        var card = ReviewTestFixtures.dueCard(user, "Run");
        card.setDueAt(java.time.Instant.parse("2026-09-07T10:00:00Z"));
        em.persist(card);
        var attempt = ReviewTestFixtures.attempt(user, card, ReviewAnswer.UNKNOWN);
        em.persist(attempt);
        em.flush();
        long originalVersion = card.getVersion();
        var due = card.getDueAt();
        var service = new com.puzzlemovies.export.service.ReviewService(cards, attempts,
                new com.puzzlemovies.export.review.BinaryReviewScheduler());
        var result = service.updateContent(user, card.getId(),
                new com.puzzlemovies.export.web.ReviewDtos.CardContentUpdateRequest("Walk", null, null, originalVersion));
        assertEquals(originalVersion + 1, result.card().version());
        em.clear();
        var saved = cards.findByIdAndUser(card.getId(), user).orElseThrow();
        assertEquals("Walk", saved.getOriginalText());
        assertEquals(due.truncatedTo(java.time.temporal.ChronoUnit.MICROS), saved.getDueAt());
        assertTrue(saved.isManualContentOverride());
        assertEquals(0, saved.getReviewCount());
        assertEquals(ReviewAnswer.UNKNOWN, attempts.findById(attempt.getId()).orElseThrow().getAnswer());
        var other = ReviewTestFixtures.user("foreign-edit@example.com");
        em.persist(other);
        assertThrows(com.puzzlemovies.export.service.ReviewService.CardNotFoundException.class,
                () -> service.updateContent(other, card.getId(),
                    new com.puzzlemovies.export.web.ReviewDtos.CardContentUpdateRequest("Foreign", null, null, saved.getVersion())));
        assertThrows(com.puzzlemovies.export.service.ReviewService.StaleCardException.class,
                () -> service.updateContent(user, card.getId(),
                    new com.puzzlemovies.export.web.ReviewDtos.CardContentUpdateRequest("Stale", null, null, originalVersion)));
        long beforeRepeat = saved.getVersion();
        var repeat = service.updateContent(user, saved.getId(),
                new com.puzzlemovies.export.web.ReviewDtos.CardContentUpdateRequest("Walk", "", "", beforeRepeat));
        assertEquals(beforeRepeat + 1, repeat.card().version());
    }

    @Test
    void databaseRejectsAnEntityLoadedBeforeAnotherUpdate() {
        var user = ReviewTestFixtures.user("race@example.com");
        em.persist(user);
        var stale = ReviewTestFixtures.dueCard(user, "Run");
        em.persist(stale);
        em.flush();
        em.detach(stale);
        var current = em.find(ReviewCard.class, stale.getId());
        current.setOriginalText("Latest");
        em.flush();
        em.clear();
        stale.setOriginalText("Outdated");
        assertThrows(jakarta.persistence.OptimisticLockException.class, () -> {
            em.merge(stale);
            em.flush();
        });
    }

    @Test
    void reviewCardHasOwnerScopedDuplicateConstraint() {
        Table table = ReviewCard.class.getAnnotation(Table.class);

        assertNotNull(table);
        assertEquals("review_cards", table.name());
        assertEquals("uk_review_cards_user_content_key", table.uniqueConstraints()[0].name());
        assertEquals("user_id", table.uniqueConstraints()[0].columnNames()[0]);
        assertEquals("content_key", table.uniqueConstraints()[0].columnNames()[1]);
    }

    @Test
    void repositoryExposesOwnerScopedDueAndDuplicateQueries() throws Exception {
        assertMethod("findByIdAndUser", 2);
        assertMethod("findByUserAndContentKey", 2);
        assertMethod("findDueCards", 3);
        assertMethod("countDueCards", 2);
        assertMethod("findNextDueAt", 2);
    }

    private void assertMethod(String name, int parameterCount) {
        for (Method method : ReviewCardRepository.class.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return;
            }
        }
        throw new AssertionError("Missing method " + name);
    }
}
