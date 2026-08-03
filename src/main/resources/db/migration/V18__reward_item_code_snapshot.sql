ALTER TABLE reward_definitions
    ADD COLUMN item_code VARCHAR(80) NULL AFTER item_id;

UPDATE reward_definitions definition
JOIN items item ON item.id = definition.item_id
SET definition.item_code = item.code
WHERE definition.reward_type = 'ITEM';

ALTER TABLE reward_definitions
    DROP CHECK ck_reward_definition_payload,
    ADD CONSTRAINT ck_reward_definition_payload CHECK (
        (
            reward_type = 'EXP'
            AND amount IS NOT NULL
            AND amount > 0
            AND item_id IS NULL
            AND item_code IS NULL
        )
        OR (
            reward_type = 'ITEM'
            AND amount IS NOT NULL
            AND amount > 0
            AND item_id IS NOT NULL
            AND item_id > 0
            AND item_code IS NOT NULL
            AND CHAR_LENGTH(TRIM(item_code)) BETWEEN 1 AND 80
        )
    );

ALTER TABLE reward_settlement_lines
    ADD COLUMN item_code VARCHAR(80) NULL AFTER item_id;

UPDATE reward_settlement_lines line
JOIN reward_definitions definition
    ON definition.id = line.reward_definition_id
SET line.item_code = definition.item_code
WHERE line.reward_type = 'ITEM';

ALTER TABLE reward_settlement_lines
    DROP CHECK ck_reward_settlement_line_payload,
    ADD CONSTRAINT ck_reward_settlement_line_payload CHECK (
        (reward_type = 'EXP' AND item_id IS NULL AND item_code IS NULL)
        OR (
            reward_type = 'ITEM'
            AND item_id IS NOT NULL
            AND item_id > 0
            AND item_code IS NOT NULL
            AND CHAR_LENGTH(TRIM(item_code)) BETWEEN 1 AND 80
        )
    );
