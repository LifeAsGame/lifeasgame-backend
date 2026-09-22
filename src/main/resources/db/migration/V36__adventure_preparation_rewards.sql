-- Account entitlement is independent of Player lifetime and content display versions.
ALTER TABLE reward_profiles ADD COLUMN entitlement_code VARCHAR(80) NULL;
ALTER TABLE reward_settlements
    ADD COLUMN account_id BIGINT NULL,
    MODIFY status ENUM ('COMPLETED','FAILED','PARTIAL_FAILED','PENDING','NOT_ELIGIBLE') NOT NULL;
CREATE TABLE account_reward_entitlements (
    account_id BIGINT NOT NULL,
    entitlement_code VARCHAR(80) NOT NULL,
    settlement_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, entitlement_code),
    CONSTRAINT uq_account_reward_settlement UNIQUE (settlement_id),
    CONSTRAINT ck_account_reward_identity CHECK (account_id > 0 AND settlement_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reward_gold_credits (
    reward_line_id BIGINT NOT NULL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_reward_gold_credit CHECK (reward_line_id > 0 AND player_id > 0 AND account_id > 0 AND amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE reward_definitions DROP CHECK ck_reward_definition_payload,
    MODIFY reward_type ENUM ('EXP','ITEM','GOLD') NOT NULL,
    ADD CONSTRAINT ck_reward_definition_payload CHECK (
        amount IS NOT NULL AND amount > 0 AND (
            (reward_type IN ('EXP','GOLD') AND item_id IS NULL AND item_code IS NULL)
            OR (reward_type = 'ITEM' AND item_id IS NOT NULL AND item_id > 0
                AND item_code IS NOT NULL AND CHAR_LENGTH(TRIM(item_code)) BETWEEN 1 AND 80)
        )
    );
ALTER TABLE reward_settlement_lines DROP CHECK ck_reward_settlement_line_payload,
    MODIFY reward_type ENUM ('EXP','ITEM','GOLD') NOT NULL,
    ADD CONSTRAINT ck_reward_settlement_line_payload CHECK (
        (reward_type IN ('EXP','GOLD') AND item_id IS NULL AND item_code IS NULL)
        OR (reward_type = 'ITEM' AND item_id IS NOT NULL AND item_id > 0
            AND item_code IS NOT NULL AND CHAR_LENGTH(TRIM(item_code)) BETWEEN 1 AND 80)
    );

ALTER TABLE items ADD COLUMN description VARCHAR(500) NULL,
    ADD COLUMN reward_bound BIT NOT NULL DEFAULT b'1';
INSERT INTO items (code, name, description, category, type, rarity, base_attrs,
                   stackable, max_stack, reward_bound, created_at, updated_at)
VALUES ('IT_RECORD_CRYSTAL', '기록 결정', '활동 기록 퀘스트에서 얻는 수집품. 보관하거나 거래할 수 있습니다.',
        'MISC', 'ETC', 'COMMON', JSON_OBJECT(), TRUE, 99, FALSE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
INSERT INTO reward_definitions (code, name, reward_type, amount, item_id, item_code, active, created_at, updated_at)
VALUES ('RD_ADVENTURE_GOLD', '모험의 준비 GOLD', 'GOLD', 100, NULL, NULL, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
INSERT INTO reward_definitions (code, name, reward_type, amount, item_id, item_code, active, created_at, updated_at)
SELECT 'RD_RECORD_CRYSTAL', '기록 결정', 'ITEM', 1, id, code, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM items WHERE code = 'IT_RECORD_CRYSTAL';
INSERT INTO reward_profiles (code, name, status, entitlement_code, created_at, updated_at)
VALUES ('RP_ADVENTURE_PREPARATION', '모험의 준비 보상', 'ACTIVE', 'ADVENTURE_PREPARATION', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
INSERT INTO reward_profile_lines (reward_profile_id, reward_definition_id, sort_order, amount_override, created_at, updated_at)
SELECT p.id, d.id, CASE d.reward_type WHEN 'GOLD' THEN 1 ELSE 2 END, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM reward_profiles p JOIN reward_definitions d ON d.code IN ('RD_ADVENTURE_GOLD','RD_RECORD_CRYSTAL')
WHERE p.code = 'RP_ADVENTURE_PREPARATION';
-- Quest bootstrap installs Q_ADVENTURE_PREPARATION from the same catalog as the existing five Quests.
