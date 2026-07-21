-- Reward definition foundation only.
-- Settlement, mailbox delivery, inventory mutation, and economy behavior are out of scope.

CREATE TABLE reward_definitions (
    active BIT NOT NULL,
    amount BIGINT,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    reward_type ENUM ('EXP','ITEM') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reward_definition_code UNIQUE (code),
    CONSTRAINT ck_reward_definition_payload CHECK (
        (reward_type = 'EXP' AND amount IS NOT NULL AND amount > 0 AND item_id IS NULL)
        OR (
            reward_type = 'ITEM'
            AND amount IS NOT NULL
            AND amount > 0
            AND item_id IS NOT NULL
            AND item_id > 0
        )
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reward_profiles (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status ENUM ('ACTIVE','INACTIVE') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reward_profile_code UNIQUE (code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reward_profile_lines (
    sort_order INTEGER NOT NULL,
    amount_override BIGINT,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    reward_definition_id BIGINT NOT NULL,
    reward_profile_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reward_profile_line_sort_order UNIQUE (reward_profile_id, sort_order),
    CONSTRAINT ck_reward_profile_line_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_reward_profile_line_amount_override CHECK (
        amount_override IS NULL OR amount_override > 0
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_reward_profile_line_definition
    ON reward_profile_lines (reward_definition_id);

ALTER TABLE reward_profile_lines
    ADD CONSTRAINT fk_reward_profile_line_profile
    FOREIGN KEY (reward_profile_id)
    REFERENCES reward_profiles (id);

ALTER TABLE reward_profile_lines
    ADD CONSTRAINT fk_reward_profile_line_definition
    FOREIGN KEY (reward_definition_id)
    REFERENCES reward_definitions (id);

-- ITEM seed is intentionally omitted because no stable catalog item id is guaranteed here.
INSERT INTO reward_definitions (
    code, name, reward_type, amount, item_id, active, created_at, updated_at
) VALUES
    ('RD_EXP_10', 'EXP 10', 'EXP', 10, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('RD_EXP_30', 'EXP 30', 'EXP', 30, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO reward_profiles (
    code, name, status, created_at, updated_at
) VALUES
    ('RP_EXP_10', 'EXP 10 Profile', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('RP_EXP_30', 'EXP 30 Profile', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO reward_profile_lines (
    reward_profile_id,
    reward_definition_id,
    sort_order,
    amount_override,
    created_at,
    updated_at
)
SELECT
    profile.id,
    definition.id,
    0,
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM reward_profiles profile
JOIN reward_definitions definition
    ON definition.code = REPLACE(profile.code, 'RP_', 'RD_')
WHERE profile.code IN ('RP_EXP_10', 'RP_EXP_30');
