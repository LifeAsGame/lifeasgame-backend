CREATE TABLE quest_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    definition_version INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TINYTEXT NULL,
    primary_role_template_code VARCHAR(80) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quest_route_code UNIQUE (code),
    CONSTRAINT ck_quest_route_code CHECK (
        CHAR_LENGTH(TRIM(code)) BETWEEN 1 AND 80
    ),
    CONSTRAINT ck_quest_route_definition_version CHECK (
        definition_version >= 1
    ),
    CONSTRAINT ck_quest_route_title CHECK (
        CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 120
    ),
    CONSTRAINT ck_quest_route_role_template CHECK (
        primary_role_template_code IS NULL
        OR CHAR_LENGTH(TRIM(primary_role_template_code)) BETWEEN 1 AND 80
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quest_route_steps (
    id BIGINT NOT NULL AUTO_INCREMENT,
    route_id BIGINT NOT NULL,
    step_code VARCHAR(80) NOT NULL,
    step_order INT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description TINYTEXT NULL,
    criterion_type VARCHAR(40) NOT NULL,
    required_evidence_count INT NOT NULL,
    user_advance_required BIT NOT NULL,
    retroactive_evidence_allowed BIT NOT NULL,
    skip_allowed BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quest_route_step_id_route UNIQUE (id, route_id),
    CONSTRAINT uq_quest_route_step_order UNIQUE (route_id, step_order),
    CONSTRAINT uq_quest_route_step_code UNIQUE (route_id, step_code),
    CONSTRAINT ck_quest_route_step_order CHECK (step_order >= 1),
    CONSTRAINT ck_quest_route_step_code CHECK (
        CHAR_LENGTH(TRIM(step_code)) BETWEEN 1 AND 80
    ),
    CONSTRAINT ck_quest_route_step_title CHECK (
        CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 120
    ),
    CONSTRAINT ck_quest_route_step_criterion CHECK (
        criterion_type = 'QUEST_COMPLETION_SET'
    ),
    CONSTRAINT ck_quest_route_step_evidence_count CHECK (
        required_evidence_count >= 1
    ),
    CONSTRAINT ck_quest_route_step_explicit_advance CHECK (
        user_advance_required = b'1'
    ),
    CONSTRAINT ck_quest_route_step_retroactive CHECK (
        retroactive_evidence_allowed = b'1'
    ),
    CONSTRAINT ck_quest_route_step_skip CHECK (
        skip_allowed = b'0'
    ),
    CONSTRAINT fk_quest_route_step_route
        FOREIGN KEY (route_id)
        REFERENCES quest_routes (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE quest_route_step_quests (
    step_id BIGINT NOT NULL,
    quest_id BIGINT NOT NULL,
    requirement_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (step_id, quest_id),
    CONSTRAINT ck_quest_route_step_quest_requirement CHECK (
        requirement_type IN ('REQUIRED', 'OPTIONAL')
    ),
    CONSTRAINT fk_quest_route_step_quest_step
        FOREIGN KEY (step_id)
        REFERENCES quest_route_steps (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_quest_route_step_quest_quest
        FOREIGN KEY (quest_id)
        REFERENCES quests (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE player_quest_routes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    current_step_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    selected_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_player_quest_route UNIQUE (player_id, route_id),
    CONSTRAINT ck_player_quest_route_player CHECK (player_id > 0),
    CONSTRAINT ck_player_quest_route_status CHECK (
        status IN ('IN_PROGRESS', 'COMPLETED')
    ),
    CONSTRAINT ck_player_quest_route_completion CHECK (
        (status = 'IN_PROGRESS' AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
    ),
    INDEX idx_player_quest_route_status (player_id, status, id),
    CONSTRAINT fk_player_quest_route_route
        FOREIGN KEY (route_id)
        REFERENCES quest_routes (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_player_quest_route_current_step
        FOREIGN KEY (current_step_id, route_id)
        REFERENCES quest_route_steps (id, route_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- The three required Quest definitions are the existing Level 1 content contract.
-- A stable code conflict preserves its existing authoritative definition.
INSERT INTO quests (
    reward_exp,
    definition_version,
    target_value,
    created_at,
    due_at,
    updated_at,
    code,
    title_id,
    reward_stats,
    reward_profile_code,
    category,
    semantic_category,
    description_md,
    repeat_rule,
    completion_policy,
    role_template_code,
    target_type,
    progress_source
) VALUES
    (
        0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6),
        'Q_RECORD_FIRST_TRACE', '첫 흔적 남기기', JSON_OBJECT(),
        'RP_EXP_TINY_10', NULL, 'RECORD',
        '오늘의 생각·행동·기억 중 하나를 짧게 남겨보세요.',
        'ONCE', 'AUTO', NULL, 'COUNT', 'RECORD_CREATED'
    ),
    (
        0, 1, 3, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6),
        'Q_RECORD_THREE_TRACES', '흔적 세 개 이어보기', JSON_OBJECT(),
        'RP_EXP_AND_ITEM_FIRST_STEP_20', NULL, 'RECORD',
        '서로 다른 순간의 기록을 세 개 남겨 작은 흐름을 만들어보세요.',
        'ONCE', 'AUTO', NULL, 'COUNT', 'RECORD_CREATED'
    ),
    (
        0, 1, 1, CURRENT_TIMESTAMP(6), NULL, CURRENT_TIMESTAMP(6),
        'Q_RECORD_WEEKLY_LOOKBACK', '이번 주 흔적 돌아보기', JSON_OBJECT(),
        'RP_NONE', NULL, 'RECORD',
        '이번 주 기록 중 하나를 골라 지금의 나에게 남길 한 줄을 적어보세요.',
        'WEEKLY', 'AUTO', NULL, 'COUNT', 'RECORD_CREATED'
    )
ON DUPLICATE KEY UPDATE code = VALUES(code);

INSERT INTO quest_routes (
    code,
    definition_version,
    title,
    description,
    primary_role_template_code,
    created_at,
    updated_at
) VALUES (
    'ROUTE_RECORD_START',
    1,
    '기록으로 시작하기',
    '작은 기록을 남기고 연결하며 돌아보는 장기 방향입니다.',
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO quest_route_steps (
    route_id,
    step_code,
    step_order,
    title,
    description,
    criterion_type,
    required_evidence_count,
    user_advance_required,
    retroactive_evidence_allowed,
    skip_allowed
)
SELECT
    route.id,
    seed.step_code,
    seed.step_order,
    seed.title,
    seed.description,
    'QUEST_COMPLETION_SET',
    1,
    b'1',
    b'1',
    b'0'
FROM quest_routes route
JOIN (
    SELECT
        'RS_RECORD_01_LEAVE_TRACE' AS step_code,
        1 AS step_order,
        '첫 흔적 남기기' AS title,
        '첫 번째 기록 Quest를 완료하고 다음 단계로 직접 이동합니다.' AS description
    UNION ALL
    SELECT
        'RS_RECORD_02_CONNECT_TRACES',
        2,
        '흔적 연결하기',
        '세 개의 기록을 연결한 뒤 다음 단계로 직접 이동합니다.'
    UNION ALL
    SELECT
        'RS_RECORD_03_LOOK_BACK',
        3,
        '돌아보기',
        '주간 회고 Quest를 완료하고 Route를 직접 완료합니다.'
) seed
WHERE route.code = 'ROUTE_RECORD_START';

INSERT INTO quest_route_step_quests (
    step_id,
    quest_id,
    requirement_type
)
SELECT step.id, quest.id, 'REQUIRED'
FROM quest_route_steps step
JOIN quest_routes route ON route.id = step.route_id
JOIN quests quest ON quest.code = CASE step.step_code
    WHEN 'RS_RECORD_01_LEAVE_TRACE' THEN 'Q_RECORD_FIRST_TRACE'
    WHEN 'RS_RECORD_02_CONNECT_TRACES' THEN 'Q_RECORD_THREE_TRACES'
    WHEN 'RS_RECORD_03_LOOK_BACK' THEN 'Q_RECORD_WEEKLY_LOOKBACK'
END
WHERE route.code = 'ROUTE_RECORD_START';
