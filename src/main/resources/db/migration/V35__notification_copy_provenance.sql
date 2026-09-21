-- Legacy rows keep their original text and NULL provenance; no retrospective copy attribution.
ALTER TABLE player_notifications
    ADD COLUMN title_copy_id VARCHAR(100) NULL,
    ADD COLUMN title_copy_version INT NULL,
    ADD COLUMN body_copy_id VARCHAR(100) NULL,
    ADD COLUMN body_copy_version INT NULL,
    ADD COLUMN copy_locale VARCHAR(20) NULL,
    ADD CONSTRAINT ck_notification_copy_provenance CHECK (
        (title_copy_id IS NULL AND title_copy_version IS NULL
            AND body_copy_id IS NULL AND body_copy_version IS NULL AND copy_locale IS NULL)
        OR
        (title_copy_id IS NOT NULL AND CHAR_LENGTH(TRIM(title_copy_id)) > 0
            AND title_copy_version IS NOT NULL AND title_copy_version > 0
            AND body_copy_id IS NOT NULL AND CHAR_LENGTH(TRIM(body_copy_id)) > 0
            AND body_copy_version IS NOT NULL AND body_copy_version > 0
            AND copy_locale IS NOT NULL AND CHAR_LENGTH(TRIM(copy_locale)) > 0)
    );
