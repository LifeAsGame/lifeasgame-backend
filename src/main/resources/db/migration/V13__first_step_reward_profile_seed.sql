-- P0 Reward definitions. The ITEM payload resolves the stable Item code from V12.
INSERT INTO reward_definitions (
    code, name, reward_type, amount, item_id, active, created_at, updated_at
) SELECT
    'RD_EXP_20',
    'EXP 20',
    'EXP',
    20,
    NULL,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM items item
WHERE item.code = 'IT_FIRST_STEP_FRAGMENT';

INSERT INTO reward_definitions (
    code, name, reward_type, amount, item_id, active, created_at, updated_at
) VALUES (
    'RD_ITEM_FIRST_STEP_FRAGMENT_1',
    'First Step Fragment x1',
    'ITEM',
    1,
    (
        SELECT item.id
        FROM items item
        WHERE item.code = 'IT_FIRST_STEP_FRAGMENT'
    ),
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO reward_profiles (
    code, name, status, created_at, updated_at
) VALUES (
    'RP_EXP_AND_ITEM_FIRST_STEP_20',
    'EXP 20 + First Step Fragment',
    'ACTIVE',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO reward_profile_lines (
    reward_profile_id,
    reward_definition_id,
    sort_order,
    amount_override,
    created_at,
    updated_at
) VALUES (
    (
        SELECT profile.id
        FROM reward_profiles profile
        WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
    ),
    (
        SELECT definition.id
        FROM reward_definitions definition
        WHERE definition.code = 'RD_EXP_20'
    ),
    0,
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO reward_profile_lines (
    reward_profile_id,
    reward_definition_id,
    sort_order,
    amount_override,
    created_at,
    updated_at
) VALUES (
    (
        SELECT profile.id
        FROM reward_profiles profile
        WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
    ),
    (
        SELECT definition.id
        FROM reward_definitions definition
        WHERE definition.code = 'RD_ITEM_FIRST_STEP_FRAGMENT_1'
    ),
    1,
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
