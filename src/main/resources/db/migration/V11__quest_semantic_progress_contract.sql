ALTER TABLE quests
    ADD COLUMN semantic_category VARCHAR(20) NULL
        AFTER category,
    ADD COLUMN progress_source VARCHAR(30) NULL
        AFTER target_type,
    ADD COLUMN role_template_code VARCHAR(80) NULL
        AFTER repeat_rule;

UPDATE quests
SET repeat_rule = 'NONE'
WHERE repeat_rule IS NULL;

-- repeat_rule remains the single persisted source of truth.
-- ONCE is the final-contract value; NONE and MONTHLY remain legacy-compatible.
ALTER TABLE quests
    MODIFY COLUMN repeat_rule ENUM (
        'ONCE',
        'DAILY',
        'WEEKLY',
        'NONE',
        'MONTHLY'
    ) NOT NULL DEFAULT 'NONE';
