ALTER TABLE quests
    ADD COLUMN completion_policy ENUM ('AUTO','USER_CONFIRM')
        NOT NULL DEFAULT 'AUTO'
        AFTER repeat_rule;

ALTER TABLE quest_acceptances
    ADD COLUMN goal_reached_at DATETIME(6) NULL
        AFTER progress_value;

ALTER TABLE quest_acceptances
    MODIFY COLUMN status ENUM (
        'IN_PROGRESS',
        'GOAL_REACHED',
        'COMPLETED',
        'DONE',
        'CANCELED'
    ) NOT NULL;

UPDATE quest_acceptances
SET status = 'COMPLETED'
WHERE status = 'DONE';

UPDATE quest_acceptances
SET goal_reached_at = completed_at
WHERE status = 'COMPLETED'
  AND goal_reached_at IS NULL;

ALTER TABLE quest_acceptances
    MODIFY COLUMN status ENUM (
        'IN_PROGRESS',
        'GOAL_REACHED',
        'COMPLETED',
        'CANCELED'
    ) NOT NULL;
