-- Only the two approved cosmetic equipment definitions become command capable.
UPDATE equipment_slots SET category = 'HEAD', role = 'SINGLE', updated_at = NOW(6)
WHERE code = 'HEAD' AND definition_version = '1.0.0' AND category IS NULL AND role IS NULL;
UPDATE equipment_slots SET category = 'CHEST', role = 'SINGLE', updated_at = NOW(6)
WHERE code = 'BODY' AND definition_version = '1.0.0' AND category IS NULL AND role IS NULL;

INSERT INTO items (code, name, description, category, type, equipment_compatibility_kind,
                   rarity, base_attrs, stackable, max_stack, max_durability, reward_bound,
                   created_at, updated_at)
VALUES ('IT_RECORD_KEEPER_HAT', '기록자의 모자', '기록을 시작한 사용자의 첫 Game Equipment',
        'ARMOR', 'HELMET', 'HEAD', 'COMMON', JSON_OBJECT(), b'0', 1, NULL, b'1', NOW(6), NOW(6)),
       ('IT_DEVELOPER_HOODIE', '개발자의 후드', '개발자 Role의 정체성 표현',
        'ARMOR', 'CHEST', 'CHEST', 'RARE', JSON_OBJECT(), b'0', 1, NULL, b'1', NOW(6), NOW(6));

-- The stable entitlement deliberately has no definition version or rule display name.
CREATE TABLE player_equipment_grant_receipts (
    player_id BIGINT NOT NULL,
    entitlement_code VARCHAR(80) NOT NULL,
    item_code VARCHAR(80) NOT NULL,
    definition_version INT NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    PRIMARY KEY (player_id, entitlement_code),
    KEY idx_equipment_grant_item (item_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
