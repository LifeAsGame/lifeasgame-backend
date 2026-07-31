ALTER TABLE quest_acceptances
    ADD COLUMN accepted_at DATETIME(6) NULL
        AFTER period_end,
    ADD COLUMN period_key VARCHAR(16) NULL
        AFTER accepted_at;

UPDATE quest_acceptances
SET accepted_at = created_at
WHERE accepted_at IS NULL;

ALTER TABLE quest_acceptances
    MODIFY COLUMN accepted_at DATETIME(6) NOT NULL;
