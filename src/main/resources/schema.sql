-- Upgrade the prototype review_cards table without losing existing study history.
-- Hibernate's ddl-auto=update cannot add required columns to a table that has rows.
DO $$
DECLARE
    has_front boolean;
    has_back boolean;
    has_repetitions boolean;
    has_lapses boolean;
BEGIN
    IF to_regclass('public.review_cards') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS original_text varchar(1000);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS instance_text varchar(4000);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS translation_text varchar(8000);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS source_context varchar(2000);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS content_key varchar(128);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS record_kind varchar(32);
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS review_count integer;
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS lapse_count integer;
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
    ALTER TABLE review_cards ADD COLUMN IF NOT EXISTS manual_content_override boolean NOT NULL DEFAULT false;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'review_cards' AND column_name = 'front'
    ) INTO has_front;
    IF has_front THEN
        EXECUTE $migration$
            UPDATE review_cards
            SET original_text = regexp_replace(front, '^(.+) \1( .*)?$', '\1'),
                translation_text = regexp_replace(front, '^(.+) \1 ?(.*)$', '\2')
            WHERE front ~ '^(.+) \1( |$)'
              AND NOT manual_content_override
              AND (translation_text IS NULL OR btrim(translation_text) = '')
        $migration$;
        EXECUTE $migration$
            UPDATE review_cards
            SET original_text = left(coalesce(nullif(btrim(front), ''), 'Legacy review card ' || id::text), 1000)
            WHERE original_text IS NULL OR btrim(original_text) = ''
        $migration$;
    ELSE
        UPDATE review_cards
        SET original_text = 'Legacy review card ' || id::text
        WHERE original_text IS NULL OR btrim(original_text) = '';
    END IF;

    -- The early prototype stored its display payload in front/back. Hibernate no longer writes
    -- those legacy columns, so they must not reject inserts for the structured card model.
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'review_cards' AND column_name = 'back'
    ) INTO has_back;
    IF has_front THEN
        ALTER TABLE review_cards ALTER COLUMN front DROP NOT NULL;
    END IF;
    IF has_back THEN
        ALTER TABLE review_cards ALTER COLUMN back DROP NOT NULL;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'review_cards' AND column_name = 'repetitions'
    ) INTO has_repetitions;
    IF has_repetitions THEN
        EXECUTE 'UPDATE review_cards SET review_count = coalesce(review_count, repetitions, 0)';
        ALTER TABLE review_cards ALTER COLUMN repetitions DROP NOT NULL;
    ELSE
        UPDATE review_cards SET review_count = coalesce(review_count, 0);
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'review_cards' AND column_name = 'lapses'
    ) INTO has_lapses;
    IF has_lapses THEN
        EXECUTE 'UPDATE review_cards SET lapse_count = coalesce(lapse_count, lapses, 0)';
        ALTER TABLE review_cards ALTER COLUMN lapses DROP NOT NULL;
    ELSE
        UPDATE review_cards SET lapse_count = coalesce(lapse_count, 0);
    END IF;

    UPDATE review_cards SET record_kind = 'WORD' WHERE record_kind IS NULL OR btrim(record_kind) = '';
    UPDATE review_cards SET content_key = 'legacy:' || id::text
    WHERE content_key IS NULL OR btrim(content_key) = '';

    ALTER TABLE review_cards ALTER COLUMN original_text SET NOT NULL;
    ALTER TABLE review_cards ALTER COLUMN content_key SET NOT NULL;
    ALTER TABLE review_cards ALTER COLUMN record_kind SET NOT NULL;
    ALTER TABLE review_cards ALTER COLUMN review_count SET NOT NULL;
    ALTER TABLE review_cards ALTER COLUMN lapse_count SET NOT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.review_cards'::regclass
          AND conname = 'uk_review_cards_user_content_key'
    ) THEN
        ALTER TABLE review_cards
        ADD CONSTRAINT uk_review_cards_user_content_key UNIQUE (user_id, content_key);
    END IF;
END $$@@
