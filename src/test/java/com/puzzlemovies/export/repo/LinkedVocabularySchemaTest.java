package com.puzzlemovies.export.repo;

import com.puzzlemovies.export.export.SourcePosition;
import com.puzzlemovies.export.model.ExportJob;
import com.puzzlemovies.export.model.ExportStatus;
import com.puzzlemovies.export.model.ExportType;
import com.puzzlemovies.export.model.ReviewAnswer;
import com.puzzlemovies.export.model.User;
import com.puzzlemovies.export.model.VocabularyExample;
import com.puzzlemovies.export.model.VocabularyImportSnapshot;
import com.puzzlemovies.export.model.WordExampleLink;
import com.puzzlemovies.export.puzzlemovies.DictionarySection;
import com.puzzlemovies.export.review.ReviewTestFixtures;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=always"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("com.puzzlemovies.export.repo.ReviewCardRepositoryTest#postgresAvailable")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LinkedVocabularySchemaTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        ReviewCardRepositoryTest.database(registry);
    }

    @Test
    void cleanStartupCreatesTheCatalog() {
        assertEquals(4, jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = current_schema()
                  and table_name in ('vocabulary_entries', 'vocabulary_examples',
                    'word_example_links', 'vocabulary_import_snapshots')
                """, Integer.class));
    }

    @Test
    void ownerForeignKeysAndUniqueEntryRemainEnforcedAfterReinitialization() {
        var owner = ReviewTestFixtures.user("schema-owner@example.invalid");
        var other = ReviewTestFixtures.user("schema-other@example.invalid");
        em.persist(owner);
        em.persist(other);
        var card = ReviewTestFixtures.dueCard(owner, "walk");
        var another = ReviewTestFixtures.dueCard(owner, "run");
        var foreign = ReviewTestFixtures.dueCard(other, "walk");
        for (var current : List.of(card, another, foreign)) {
            em.persist(current.getVocabularyEntry());
            em.persist(current);
        }
        var example = example(owner);
        var foreignExample = example(other);
        var link = new WordExampleLink(owner, card.getVocabularyEntry(), example,
                WordExampleLink.MatchKind.LEMMA_SEQUENCE, 1);
        em.persist(link);
        var secondLink = new WordExampleLink(owner, another.getVocabularyEntry(), example,
                WordExampleLink.MatchKind.LEMMA_SEQUENCE, 1);
        em.persist(secondLink);
        var attempt = ReviewTestFixtures.attempt(owner, card, ReviewAnswer.KNOWN);
        em.persist(attempt);
        var job = job(owner);
        var foreignJob = job(other);
        var snapshot = new VocabularyImportSnapshot(owner, job,
                new VocabularyImportSnapshot.Payload(List.of(), List.of()), "0".repeat(64));
        em.persist(snapshot);
        em.flush();
        em.unwrap(Session.class).doWork(connection -> ScriptUtils.executeSqlScript(connection,
                new org.springframework.core.io.support.EncodedResource(new ClassPathResource("schema.sql")),
                false, false, ScriptUtils.DEFAULT_COMMENT_PREFIX, "@@",
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER, ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER));
        assertEquals(3, jdbc.queryForObject("select count(*) from review_cards", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from word_example_links", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from review_attempts", Integer.class));
        assertEquals(1, jdbc.queryForObject("select count(*) from vocabulary_import_snapshots", Integer.class));
        rejected("23505", "update review_cards set vocabulary_entry_id = ? where id = ?",
                card.getVocabularyEntry().getId(), another.getId());
        rejected("23503", "update review_cards set vocabulary_entry_id = ? where id = ?",
                foreign.getVocabularyEntry().getId(), card.getId());
        rejected("23503", "update review_attempts set review_card_id = ? where id = ?", foreign.getId(), attempt.getId());
        rejected("23503", "update word_example_links set entry_id = ? where id = ?",
                foreign.getVocabularyEntry().getId(), link.getId());
        rejected("23505", "update word_example_links set entry_id = ? where id = ?",
                card.getVocabularyEntry().getId(), secondLink.getId());
        rejected("23503", "update word_example_links set example_id = ? where id = ?", foreignExample.getId(), link.getId());
        rejected("23503", "update vocabulary_import_snapshots set export_job_id = ? where id = ?",
                foreignJob.getId(), snapshot.getId());
    }

    private void rejected(String state, String sql, UUID value, UUID id) {
        em.unwrap(Session.class).doWork(connection -> {
            var savepoint = connection.setSavepoint();
            try (var statement = connection.prepareStatement(sql)) {
                statement.setObject(1, value);
                statement.setObject(2, id);
                assertEquals(state, assertThrows(SQLException.class, statement::executeUpdate).getSQLState());
            } finally {
                connection.rollback(savepoint);
                connection.releaseSavepoint(savepoint);
            }
        });
    }

    private VocabularyExample example(User owner) {
        var example = new VocabularyExample(owner, "example-key", "example", 0);
        example.replaceSource("Let's walk.", List.of(), null, null, null,
                new VocabularyExample.Provenance(1, UUID.randomUUID(),
                        new SourcePosition(DictionarySection.PHRASE, 1, 1, null), null, null),
                new VocabularyExample.MatchingSentences(1, List.of(List.of("let's", "walk")), false));
        em.persist(example);
        return example;
    }

    private ExportJob job(User owner) {
        var job = new ExportJob();
        job.setUser(owner);
        job.setType(ExportType.WORDS);
        job.setStatus(ExportStatus.COMPLETED);
        em.persist(job);
        return job;
    }
}
