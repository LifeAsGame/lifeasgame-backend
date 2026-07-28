-- Stable content code remains nullable so existing Item rows and the legacy create API keep working.
ALTER TABLE items
    ADD COLUMN code VARCHAR(80) NULL AFTER id,
    ADD CONSTRAINT uq_item_code UNIQUE (code);

-- Runtime mapping only. Description and MEMORY_FRAGMENT content metadata are not modeled yet.
INSERT INTO items (
    code,
    name,
    category,
    type,
    rarity,
    base_attrs,
    stackable,
    max_stack,
    max_durability,
    created_at,
    updated_at
) VALUES (
    'IT_FIRST_STEP_FRAGMENT',
    '첫걸음의 조각',
    'QUEST',
    'ETC',
    'COMMON',
    JSON_OBJECT(),
    TRUE,
    99,
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
