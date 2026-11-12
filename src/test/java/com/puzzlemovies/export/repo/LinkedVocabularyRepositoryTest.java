package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.config.LemmatizerConfig;
import com.puzzlemovies.export.export.DictionaryMatcher;
import com.puzzlemovies.export.export.RawDictionaryEntry;
import com.puzzlemovies.export.export.SourceExample;
import com.puzzlemovies.export.export.SourcePosition;
import com.puzzlemovies.export.export.VocabularyNormalizer;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.ReviewCardState;
import com.puzzlemovies.export.model.VocabularyEntry;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import com.puzzlemovies.export.service.LinkedVocabularyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("com.puzzlemovies.export.repo.ReviewCardRepositoryTest#postgresAvailable")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({LinkedVocabularyService.class, DictionaryMatcher.class, VocabularyNormalizer.class, LemmatizerConfig.class})
class LinkedVocabularyRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    @Autowired LinkedVocabularyService catalog;
    @Autowired VocabularyEntryRepository entries;
    @Autowired VocabularyExampleRepository examples;
    @Autowired WordExampleLinkRepository links;
    @Autowired ReviewCardRepository cards;
    @Autowired ReviewAttemptRepository attempts;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        ReviewCardRepositoryTest.database(registry);
    }

    @Test
    void republishesWithoutDuplicateCardsOrLinksAndKeepsOwnersIsolated() {
        var owner = ReviewTestFixtures.user("vocab-owner@example.invalid");
        var other = ReviewTestFixtures.user("vocab-other@example.invalid");
        em.persist(owner);
        em.persist(other);
        var phraseA = phrase(null, "I walk home.", "Film A", "Я иду домой.");
        var phraseB = phrase(null, "I walk home.", "Film B", "Я иду домой.");
        var source = List.of(word("walk", List.of("идти"),
                        List.of(new SourceExample("I left the house.", List.of(), null, null, null))),
                word(" Walk ", List.of("гулять"), List.of()),
                word("walk home", List.of(), List.of()), phraseA, phraseB, phraseA);
        assertEquals(2, catalog.publish(catalog.lockOwner(owner.getId()), UUID.randomUUID(), source, NOW));
        assertEquals(0, catalog.publish(catalog.lockOwner(other.getId()), UUID.randomUUID(), List.of(phraseB), NOW));
        assertEquals(0, catalog.publish(catalog.lockOwner(owner.getId()), UUID.randomUUID(), source, NOW));
        em.flush();
        em.clear();

        var walk = entry(owner.getId(), "walk");
        var walkHome = entry(owner.getId(), "walk home");
        assertEquals(List.of("идти", "гулять"), walk.getSourceTranslations());
        assertEquals(3, examples.findByUser_IdOrderByFirstSeenOrderAscIdAsc(owner.getId()).size());
        assertEquals(1, examples.findByUser_IdOrderByFirstSeenOrderAscIdAsc(other.getId()).size());
        var walkLinks = links.findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(owner.getId(), walk.getId());
        var homeLinks = links.findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(owner.getId(), walkHome.getId());
        assertEquals(3, walkLinks.size());
        assertEquals("I left the house.", walkLinks.getFirst().getEffectiveOriginal());
        assertEquals(List.of("Film A", "Film B"), homeLinks.stream().map(link -> link.getExample().getMovieTitle()).toList());
        assertEquals(walkLinks.get(1).getExample().getId(), homeLinks.getFirst().getExample().getId());
        assertTrue(links.findByIdAndUser_Id(walkLinks.getFirst().getId(), other.getId()).isEmpty());
        var due = cards.findDueCards(owner, NOW, PageRequest.of(0, 10));
        assertEquals(2, due.size());
        assertTrue(due.stream().allMatch(card -> card.getState() == ReviewCardState.NEW && card.getReviewCount() == 0));
        assertTrue(cards.findEntryIdsByUserId(other.getId()).isEmpty());
    }

    @Test
    void sourceUpdateKeepsCustomizedContextAndInvalidatesAllAffectedCards() {
        var owner = ReviewTestFixtures.user("vocab-edit@example.invalid");
        em.persist(owner);
        var walk = word("walk", List.of("идти"), List.of());
        var walkHome = word("walk home", List.of("идти домой"), List.of());
        catalog.publish(catalog.lockOwner(owner.getId()), UUID.randomUUID(),
                List.of(walk, walkHome, phrase("stable", "I walk home.", "A film", "Иду домой.")), NOW);
        var first = entry(owner.getId(), "walk");
        var second = entry(owner.getId(), "walk home");
        var link = links.findByUser_IdAndEntry_IdOrderByExample_FirstSeenOrderAscExample_IdAsc(
                owner.getId(), first.getId()).getFirst();
        link.setTranslationOverride("");
        first.setOriginalOverride("take a stroll");
        first.setTranslationOverride("прогуливаться");
        var card = cards.findDueCards(owner, NOW, PageRequest.of(0, 10)).stream()
                .filter(candidate -> candidate.getVocabularyEntry().getId().equals(first.getId())).findFirst().orElseThrow();
        card.setReviewCount(7);
        card.setDueAt(NOW.plusSeconds(3600));
        em.persist(ReviewTestFixtures.attempt(owner, card, ReviewAnswer.KNOWN));
        long firstRevision = first.getContentRevision();
        long secondRevision = second.getContentRevision();
        UUID sourceId = link.getExample().getId();
        var secondLink = links.findByUser_IdAndEntry_IdAndExample_Id(owner.getId(), second.getId(), sourceId).orElseThrow();
        long secondLinkVersion = secondLink.getVersion();

        assertEquals(0, catalog.publish(catalog.lockOwner(owner.getId()), UUID.randomUUID(),
                List.of(phrase("stable", "We walk home.", "Updated film", "Мы идём домой.")), NOW));
        em.flush();
        em.clear();

        var saved = links.findByIdAndUser_Id(link.getId(), owner.getId()).orElseThrow();
        assertEquals(sourceId, saved.getExample().getId());
        assertEquals("We walk home.", saved.getEffectiveOriginal());
        assertEquals("", saved.getEffectiveTranslation());
        assertEquals("Updated film", saved.getExample().getMovieTitle());
        assertTrue(saved.isCustomized());
        var savedEntry = entry(owner.getId(), "walk");
        assertEquals("take a stroll", savedEntry.getEffectiveOriginal());
        assertEquals("прогуливаться", savedEntry.getEffectiveTranslation());
        assertTrue(savedEntry.getContentRevision() > firstRevision);
        assertTrue(entry(owner.getId(), "walk home").getContentRevision() > secondRevision);
        assertTrue(links.findById(secondLink.getId()).orElseThrow().getVersion() > secondLinkVersion);
        var savedCard = cards.findByIdAndUser(card.getId(), owner).orElseThrow();
        assertEquals(NOW.plusSeconds(3600), savedCard.getDueAt());
        assertEquals(7, savedCard.getReviewCount());
        assertEquals(1, attempts.findByReviewCardAndUserOrderByReviewedAtAsc(savedCard, owner).size());
        assertEquals(1, examples.findByUser_IdOrderByFirstSeenOrderAscIdAsc(owner.getId()).size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentPublicationsForTheSameOwnerSerializeBeforeWriting() throws Exception {
        var transaction = new TransactionTemplate(transactions);
        var owner = ReviewTestFixtures.user("vocab-concurrent@example.invalid");
        transaction.executeWithoutResult(status -> em.persist(owner));
        var rows = List.of(word("walk", List.of(), List.of()), phrase(null, "I walk home.", "A film", ""));
        var firstLocked = new CountDownLatch(1);
        var publishFirst = new CountDownLatch(1);
        var secondBackend = new CompletableFuture<Integer>();

        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> transaction.execute(status -> {
                var locked = catalog.lockOwner(owner.getId());
                firstLocked.countDown();
                await(publishFirst);
                return catalog.publish(locked, UUID.randomUUID(), rows, NOW);
            }));
            try {
                await(firstLocked);
                var second = workers.submit(() -> transaction.execute(status -> {
                    secondBackend.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                    return catalog.publish(catalog.lockOwner(owner.getId()), UUID.randomUUID(), rows, NOW);
                }));
                int backend = secondBackend.get(5, TimeUnit.SECONDS);
                assertTrue(waitingForLock(backend), "Second publication must wait for the first owner's row lock");
                publishFirst.countDown();
                assertEquals(1, first.get(5, TimeUnit.SECONDS));
                assertEquals(0, second.get(5, TimeUnit.SECONDS));
            } finally {
                publishFirst.countDown();
            }
        }
        assertEquals(1, cards.findEntryIdsByUserId(owner.getId()).size());
        assertEquals(1, links.findByUser_Id(owner.getId()).size());
    }

    private boolean waitingForLock(int backend) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    select exists (select 1 from pg_stat_activity where pid = ? and wait_event_type = 'Lock')
                    """, Boolean.class, backend))) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Worker synchronization timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private VocabularyEntry entry(UUID userId, String identity) {
        return entries.findByUser_IdOrderByIdAsc(userId).stream()
                .filter(entry -> entry.getSourceIdentity().equals(identity)).findFirst().orElseThrow();
    }

    private static RawDictionaryEntry word(String text, List<String> translations, List<SourceExample> inline) {
        return new RawDictionaryEntry(new SourcePosition(DictionarySection.WORD, 1, 1, null),
                text, translations, inline, null, null, null);
    }

    private static RawDictionaryEntry phrase(String id, String text, String movie, String translation) {
        return new RawDictionaryEntry(new SourcePosition(DictionarySection.PHRASE, 1, 1, id),
                text, List.of(translation), List.of(), null, movie, null);
    }
}
