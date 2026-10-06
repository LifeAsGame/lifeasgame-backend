-- The existing definition rows and holder timestamps are never overwritten.
ALTER TABLE achievements ADD COLUMN definition_version INT NOT NULL DEFAULT 1;
ALTER TABLE titles ADD COLUMN definition_version INT NOT NULL DEFAULT 1;

CREATE TABLE achievement_award_receipts (
    player_id BIGINT NOT NULL,
    achievement_code VARCHAR(60) NOT NULL,
    source_kind VARCHAR(40),
    source_key VARCHAR(128),
    source_event_id CHAR(36),
    source_occurred_at DATETIME(6),
    provenance VARCHAR(32),
    rule_version INT NOT NULL DEFAULT 1,
    processed_at DATETIME(6) NOT NULL,
    status VARCHAR(24) NOT NULL,
    linked_title_code VARCHAR(60),
    title_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (player_id, achievement_code),
    UNIQUE KEY uq_award_source_event (source_event_id),
    KEY idx_award_source (source_kind, source_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE title_award_blocks (
    player_id BIGINT NOT NULL,
    title_code VARCHAR(60) NOT NULL,
    revoked_at DATETIME(6) NOT NULL,
    PRIMARY KEY (player_id, title_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO achievements (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'ACH_FIRST_LIFELOG', '첫 기록', 'STORY', '내용이 완성된 첫 사용자 LifeLog를 기록한다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM achievements WHERE code = 'ACH_FIRST_LIFELOG');
INSERT INTO achievements (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'ACH_FIRST_QUEST_COMPLETE', '첫 퀘스트 완료', 'STORY', '첫 퀘스트를 실제로 완료한다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM achievements WHERE code = 'ACH_FIRST_QUEST_COMPLETE');
INSERT INTO achievements (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'ACH_FIRST_ITEM_CLAIM', '첫 아이템 수령', 'COLLECTION', '우편 아이템을 처음 Inventory로 수령한다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM achievements WHERE code = 'ACH_FIRST_ITEM_CLAIM');
INSERT INTO achievements (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'ACH_ROUTE_RECORD_START', '기록 여정 완주', 'STORY', '기록 여정의 마지막 단계를 직접 진행하여 완료한다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM achievements WHERE code = 'ACH_ROUTE_RECORD_START');
INSERT INTO achievements (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'ACH_ROUTE_BACKEND_START', '백엔드 개발자 여정 완주', 'STORY', '백엔드 개발자 여정의 마지막 단계를 직접 진행하여 완료한다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM achievements WHERE code = 'ACH_ROUTE_BACKEND_START');

INSERT INTO titles (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'TITLE_CANDIDATE_RECORD_BEGINNER', '기록의 시작', 'ACHIEVEMENT', '첫 기록 업적을 얻는다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM titles WHERE code = 'TITLE_CANDIDATE_RECORD_BEGINNER');
INSERT INTO titles (code, name, category, desc_md, definition_version, created_at, updated_at)
SELECT 'TITLE_BACKEND_GUIDE', '백엔드 길잡이', 'ACHIEVEMENT', '백엔드 개발자 여정 완주 업적을 얻는다.', 1, NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM titles WHERE code = 'TITLE_BACKEND_GUIDE');
