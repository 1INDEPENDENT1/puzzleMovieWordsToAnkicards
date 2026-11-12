-- Hibernate creates the mapped tables and unique keys before this initializer runs.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'review_cards'
          AND column_name IN ('content_key', 'front', 'original_text')
    ) THEN
        RAISE EXCEPTION 'The former review schema requires the one-time preparation in specs/008-linked-card-examples/fresh-start.md';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'word_example_links'::regclass
                   AND conname = 'fk_word_example_links_owner_entry') THEN
        ALTER TABLE word_example_links ADD CONSTRAINT fk_word_example_links_owner_entry
            FOREIGN KEY (user_id, entry_id) REFERENCES vocabulary_entries (user_id, id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'word_example_links'::regclass
                   AND conname = 'fk_word_example_links_owner_example') THEN
        ALTER TABLE word_example_links ADD CONSTRAINT fk_word_example_links_owner_example
            FOREIGN KEY (user_id, example_id) REFERENCES vocabulary_examples (user_id, id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'review_cards'::regclass
                   AND conname = 'fk_review_cards_owner_entry') THEN
        ALTER TABLE review_cards ADD CONSTRAINT fk_review_cards_owner_entry
            FOREIGN KEY (user_id, vocabulary_entry_id) REFERENCES vocabulary_entries (user_id, id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'review_attempts'::regclass
                   AND conname = 'fk_review_attempts_owner_card') THEN
        ALTER TABLE review_attempts ADD CONSTRAINT fk_review_attempts_owner_card
            FOREIGN KEY (user_id, review_card_id) REFERENCES review_cards (user_id, id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'vocabulary_import_snapshots'::regclass
                   AND conname = 'fk_vocabulary_import_snapshots_owner_job') THEN
        ALTER TABLE vocabulary_import_snapshots ADD CONSTRAINT fk_vocabulary_import_snapshots_owner_job
            FOREIGN KEY (user_id, export_job_id) REFERENCES export_jobs (user_id, id) ON DELETE RESTRICT;
    END IF;
END $$@@
