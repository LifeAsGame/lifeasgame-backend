-- Reward settlement persistence foundation only.
-- No EXP grant, item delivery, mailbox, quest completion, or retry behavior is executed here.

CREATE TABLE reward_settlements (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    reward_profile_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    reward_profile_code VARCHAR(80) NOT NULL,
    source_type ENUM ('QUEST_COMPLETION') NOT NULL,
    status ENUM ('COMPLETED','FAILED','PARTIAL_FAILED','PENDING') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reward_settlement_source UNIQUE (player_id, source_type, source_id),
    CONSTRAINT ck_reward_settlement_player_id CHECK (player_id > 0),
    CONSTRAINT ck_reward_settlement_profile_id CHECK (reward_profile_id > 0),
    CONSTRAINT ck_reward_settlement_source_id CHECK (source_id > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_reward_settlement_profile
    ON reward_settlements (reward_profile_id);

CREATE TABLE reward_settlement_lines (
    sort_order INTEGER NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT,
    reward_definition_id BIGINT NOT NULL,
    reward_settlement_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    failure_code VARCHAR(100),
    reward_definition_code VARCHAR(80) NOT NULL,
    reward_type ENUM ('EXP','ITEM') NOT NULL,
    status ENUM ('FAILED','PENDING','SUCCEEDED') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reward_settlement_line_sort_order UNIQUE (reward_settlement_id, sort_order),
    CONSTRAINT ck_reward_settlement_line_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_reward_settlement_line_amount CHECK (amount > 0),
    CONSTRAINT ck_reward_settlement_line_payload CHECK (
        (reward_type = 'EXP' AND item_id IS NULL)
        OR (reward_type = 'ITEM' AND item_id IS NOT NULL AND item_id > 0)
    ),
    CONSTRAINT ck_reward_settlement_line_failure CHECK (
        (status = 'FAILED' AND failure_code IS NOT NULL)
        OR (status <> 'FAILED' AND failure_code IS NULL)
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_reward_settlement_line_definition
    ON reward_settlement_lines (reward_definition_id);

ALTER TABLE reward_settlement_lines
    ADD CONSTRAINT fk_reward_settlement_line_settlement
    FOREIGN KEY (reward_settlement_id)
    REFERENCES reward_settlements (id);
