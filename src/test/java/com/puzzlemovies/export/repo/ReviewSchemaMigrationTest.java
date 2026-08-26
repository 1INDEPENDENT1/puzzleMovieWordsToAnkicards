package com.puzzlemovies.export.repo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewSchemaMigrationTest {
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
