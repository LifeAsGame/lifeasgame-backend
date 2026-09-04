ALTER TABLE equipment_slots
    MODIFY COLUMN category ENUM (
        'CHEST','FEET','HANDS','HEAD','LEGS','NECK','RING','TRINKET','WEAPON'
    ) NULL,
    MODIFY COLUMN role ENUM (
        'LEFT','MAIN','OFFHAND','RIGHT','SINGLE'
    ) NULL,
    ADD COLUMN definition_version VARCHAR(20) NOT NULL DEFAULT 'LEGACY'
        AFTER role,
    ADD COLUMN logical_category VARCHAR(20) NULL
        AFTER definition_version,
    ADD COLUMN semantic_role VARCHAR(500) NULL
        AFTER logical_category,
    ADD COLUMN release_tier VARCHAR(2) NULL
        AFTER semantic_role,
    ADD COLUMN sort_order INT NULL
        AFTER release_tier,
    ADD COLUMN enabled BIT NOT NULL DEFAULT b'0'
        AFTER sort_order,
    ADD COLUMN lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'GATED'
        AFTER enabled,
    ADD COLUMN introduced_activation_wave VARCHAR(40) NULL
        AFTER lifecycle_status,
    ADD COLUMN replacement_slot_code VARCHAR(40) NULL
        AFTER introduced_activation_wave,
    ADD COLUMN source_revision VARCHAR(200) NULL
        AFTER replacement_slot_code,
    ADD COLUMN approved_by VARCHAR(200) NULL
        AFTER source_revision,
    ADD COLUMN eager_on_link_start BIT NOT NULL DEFAULT b'0'
        AFTER approved_by;

INSERT INTO equipment_slots (
    created_at,
    updated_at,
    code,
    name,
    category,
    role,
    definition_version,
    logical_category,
    semantic_role,
    release_tier,
    sort_order,
    enabled,
    lifecycle_status,
    introduced_activation_wave,
    replacement_slot_code,
    source_revision,
    approved_by,
    eager_on_link_start
) VALUES
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'HEAD', '머리', NULL, NULL, '1.0.0', 'AVATAR',
        'Cap/Hat/Hood/Helmet/Crown/Headband를 통합한 캐릭터 머리 외형 슬롯',
        'P0', 10, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'FACE', '얼굴', NULL, NULL, '1.0.0', 'AVATAR',
        'Glasses/Mask/Monocle용 캐릭터 얼굴 오버레이 슬롯',
        'P1', 20, b'0', 'GATED', 'C5_P1_JOURNEY', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'NECK', '목', NULL, NULL, '1.0.0', 'AVATAR',
        'Necklace/Amulet/Scarf를 통합한 목·상체 장식 슬롯',
        'P0', 30, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'BODY', '의상', NULL, NULL, '1.0.0', 'AVATAR',
        'Jacket/Robe/Armor Cosmetic/Uniform/Hoodie용 주 캐릭터 의상 슬롯',
        'P0', 40, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'BACK', '등', NULL, NULL, '1.0.0', 'AVATAR',
        'Cloak/Cape/Backpack/Wing Cosmetic용 등 장식 레이어 슬롯',
        'P1', 50, b'0', 'GATED', 'C5_P1_JOURNEY', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'HANDS', '손', NULL, NULL, '1.0.0', 'AVATAR',
        '좌우를 분리하지 않는 Gloves용 양손 공통 장식 슬롯',
        'P1', 60, b'0', 'GATED', 'C5_P1_JOURNEY', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'WRIST', '손목', NULL, NULL, '1.0.0', 'AVATAR',
        'Wristband/Bracelet용 손목 액세서리 슬롯',
        'P0', 70, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'RING_LEFT', '반지', NULL, NULL, '1.0.0', 'AVATAR',
        'P0에서 단일 반지로 표시되는 primary Ring/Signet/Route Ring 슬롯',
        'P0', 80, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'RING_RIGHT', '두 번째 반지', NULL, NULL, '1.0.0', 'AVATAR',
        '별도 호환 규칙이 승인된 이후 사용하는 두 번째 반지 슬롯',
        'P1', 90, b'0', 'GATED', 'C5_P1_JOURNEY', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'FEET', '신발', NULL, NULL, '1.0.0', 'AVATAR',
        'Shoes/Boots/Sneakers/Slippers를 통합한 신발 슬롯',
        'P0', 100, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'AURA', '오라', NULL, NULL, '1.0.0', 'PROFILE',
        '프로필·완료 결과 주변의 Aura/Trail/Glow 표현 슬롯',
        'P0', 110, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'PROFILE_FRAME', '프로필 프레임', NULL, NULL, '1.0.0', 'PROFILE',
        '아바타 초상화 테두리를 표현하는 Profile Frame 슬롯',
        'P0', 120, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'BADGE', '대표 배지', NULL, NULL, '1.0.0', 'PROFILE',
        '획득 Badge Item 중 하나를 대표로 표시하는 Profile Badge 슬롯',
        'P0', 130, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'1'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'TITLE', '대표 칭호', NULL, NULL, '1.0.0', 'IDENTITY',
        '보유 Title Entitlement 중 대표 0개 또는 1개를 선택하는 Identity 슬롯; Inventory Item 아님',
        'P0', 140, b'1', 'ACTIVE', 'C1_ONBOARDING', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'COMPANION', '동행', NULL, NULL, '1.0.0', 'COMPANION',
        '능력치·자동 Quest 없이 Cosmetic Companion 하나를 소환하는 상태 슬롯',
        'P2', 150, b'0', 'GATED', 'C6_P2', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'WEAPON_COSMETIC', '주 장식', NULL, NULL, '1.0.0', 'AVATAR',
        '전투 기능 없이 Sword/Staff/Guitar/Keyboard 형태를 표현하는 주 장식 슬롯',
        'P1', 160, b'0', 'GATED', 'C5_P1_JOURNEY', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    ),
    (
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
        'OFFHAND_COSMETIC', '보조 장식', NULL, NULL, '1.0.0', 'AVATAR',
        '주 장식과 충돌 규칙이 필요한 Book/Shield/Secondary Prop 보조 슬롯',
        'P2', 170, b'0', 'GATED', 'C6_P2', NULL,
        'GS2A_FINAL+CS2B_FINAL_CORRECTED_2026-07-23+EQSA_V2_2026-09-05',
        'CharacterInventoryProductAuthority|ContentGameSystemAuthority', b'0'
    )
ON DUPLICATE KEY UPDATE
    updated_at = VALUES(updated_at),
    name = VALUES(name),
    definition_version = VALUES(definition_version),
    logical_category = VALUES(logical_category),
    semantic_role = VALUES(semantic_role),
    release_tier = VALUES(release_tier),
    sort_order = VALUES(sort_order),
    enabled = VALUES(enabled),
    lifecycle_status = VALUES(lifecycle_status),
    introduced_activation_wave = VALUES(introduced_activation_wave),
    replacement_slot_code = VALUES(replacement_slot_code),
    source_revision = VALUES(source_revision),
    approved_by = VALUES(approved_by),
    eager_on_link_start = VALUES(eager_on_link_start);

ALTER TABLE equipment_slots
    DROP INDEX UK3580sud7mp8k4j0gobcyqot4l,
    ADD CONSTRAINT uq_equipment_slot_code_version
        UNIQUE (code, definition_version),
    ADD CONSTRAINT uq_equipment_slot_version_sort
        UNIQUE (definition_version, sort_order),
    ADD CONSTRAINT ck_equipment_slot_lifecycle_enabled CHECK (
        (lifecycle_status = 'ACTIVE' AND enabled = b'1')
        OR (
            lifecycle_status IN ('GATED', 'DEPRECATED', 'RETIRED')
            AND enabled = b'0'
        )
    ),
    ADD CONSTRAINT ck_equipment_slot_release_tier CHECK (
        release_tier IS NULL OR release_tier IN ('P0', 'P1', 'P2')
    ),
    ADD CONSTRAINT ck_equipment_slot_logical_category CHECK (
        logical_category IS NULL
        OR logical_category IN ('AVATAR', 'PROFILE', 'IDENTITY', 'COMPANION')
    ),
    ADD CONSTRAINT ck_equipment_slot_eager_provisioning CHECK (
        eager_on_link_start = b'0'
        OR (
            definition_version = '1.0.0'
            AND release_tier = 'P0'
            AND lifecycle_status = 'ACTIVE'
            AND enabled = b'1'
        )
    ),
    ADD CONSTRAINT ck_equipment_slot_authority_metadata CHECK (
        definition_version = 'LEGACY'
        OR (
            logical_category IS NOT NULL
            AND semantic_role IS NOT NULL
            AND release_tier IS NOT NULL
            AND sort_order IS NOT NULL
            AND introduced_activation_wave IS NOT NULL
            AND source_revision IS NOT NULL
            AND approved_by IS NOT NULL
        )
    ),
    ADD INDEX idx_equipment_slot_provisioning (
        definition_version,
        enabled,
        lifecycle_status,
        eager_on_link_start,
        sort_order
    );
