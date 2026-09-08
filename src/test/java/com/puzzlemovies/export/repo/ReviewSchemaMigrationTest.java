package com.puzzlemovies.export.repo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewSchemaMigrationTest {
    @org.junit.jupiter.api.Nested
    @org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest(properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"})
    @org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace =
            org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
    @org.junit.jupiter.api.condition.EnabledIf("com.puzzlemovies.export.repo.ReviewCardRepositoryTest#postgresAvailable")
    class PostgreSqlMigration {
        @org.springframework.beans.factory.annotation.Autowired jakarta.persistence.EntityManager em;

        @org.springframework.test.context.DynamicPropertySource
        static void database(org.springframework.test.context.DynamicPropertyRegistry registry) {
            ReviewCardRepositoryTest.database(registry);
        }

        @Test
        void upgradesExistingRowsIdempotentlyAndPreservesManualLegacyCorrections() throws Exception {
            var user = com.puzzlemovies.export.review.ReviewTestFixtures.user("migration@example.com");
            em.persist(user);
            var card = com.puzzlemovies.export.review.ReviewTestFixtures.dueCard(user, "Existing");
            em.persist(card);
            em.flush();
            String sql = Files.readString(Path.of("src/main/resources/schema.sql")).replace("@@", "");
            em.unwrap(org.hibernate.Session.class).doWork(connection -> {
                try (var statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE review_cards DROP COLUMN version, DROP COLUMN manual_content_override");
                    statement.execute(sql);
                    try (var result = statement.executeQuery("SELECT version, manual_content_override, original_text FROM review_cards")) {
                        assertTrue(result.next());
                        org.junit.jupiter.api.Assertions.assertEquals(0, result.getLong(1));
                        org.junit.jupiter.api.Assertions.assertFalse(result.getBoolean(2));
                        org.junit.jupiter.api.Assertions.assertEquals("Existing", result.getString(3));
                    }
                    statement.execute("ALTER TABLE review_cards ADD COLUMN front text");
                    statement.execute("UPDATE review_cards SET front='Run Run translation', original_text='My correction', translation_text='', manual_content_override=true, version=7");
                    statement.execute(sql);
                    statement.execute(sql);
                    try (var result = statement.executeQuery("SELECT version, original_text, translation_text FROM review_cards")) {
                        assertTrue(result.next());
                        org.junit.jupiter.api.Assertions.assertEquals(7, result.getLong(1));
                        org.junit.jupiter.api.Assertions.assertEquals("My correction", result.getString(2));
                        org.junit.jupiter.api.Assertions.assertEquals("", result.getString(3));
                    }
                }
            });
        }
    }

    @Test
    void addsEditSafetyDefaultsForExistingRows() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/schema.sql"));
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0"));
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS manual_content_override boolean NOT NULL DEFAULT false"));
    }

    @Test
    void migratesLegacyReviewCardsBeforeHibernateUpdatesTheSchema() throws Exception {
        String config = Files.readString(Path.of("src/main/resources/application.yml"));
        String migration = Files.readString(Path.of("src/main/resources/schema.sql"));

        assertTrue(config.contains("mode: always"));
        assertTrue(config.contains("separator: \"@@\""));
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS original_text"));
        assertTrue(migration.contains("ADD COLUMN IF NOT EXISTS content_key"));
        assertTrue(migration.contains("repetitions"));
        assertTrue(migration.contains("lapses"));
        assertTrue(migration.contains("regexp_replace(front, '^(.+) \\1( .*)?$', '\\1')"));
        assertTrue(migration.contains("translation_text = regexp_replace"));
        assertTrue(migration.contains("ALTER COLUMN front DROP NOT NULL"));
        assertTrue(migration.contains("ALTER COLUMN back DROP NOT NULL"));
        assertTrue(migration.contains("ALTER COLUMN repetitions DROP NOT NULL"));
        assertTrue(migration.contains("ALTER COLUMN lapses DROP NOT NULL"));
        assertTrue(migration.contains("uk_review_cards_user_content_key"));
        assertTrue(migration.trim().endsWith("END $$@@"));
    }
}
