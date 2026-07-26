ALTER TABLE quests
    ADD COLUMN definition_version INT NOT NULL DEFAULT 1
        AFTER reward_exp,
    ADD COLUMN reward_profile_code VARCHAR(80) NULL
        AFTER reward_stats,
    ADD CONSTRAINT ck_quest_definition_version
        CHECK (definition_version >= 1);

CREATE INDEX idx_quest_reward_profile_code
    ON quests (reward_profile_code);
