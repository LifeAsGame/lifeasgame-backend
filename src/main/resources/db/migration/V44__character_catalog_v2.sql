ALTER TABLE certification
    MODIFY COLUMN issuer VARCHAR(255) NULL,
    ADD COLUMN provider VARCHAR(32) NULL,
    ADD COLUMN source_code VARCHAR(64) NULL,
    ADD COLUMN major_code VARCHAR(32) NULL,
    ADD COLUMN major_name VARCHAR(120) NULL,
    ADD COLUMN minor_code VARCHAR(32) NULL,
    ADD COLUMN minor_name VARCHAR(120) NULL,
    ADD COLUMN administering_agency VARCHAR(255) NULL,
    ADD COLUMN detail TEXT NULL,
    ADD COLUMN detail_status VARCHAR(20) NULL,
    ADD COLUMN source_url VARCHAR(500) NULL,
    ADD COLUMN fetched_at DATETIME(6) NULL,
    ADD COLUMN active BIT NOT NULL DEFAULT b'1',
    ADD CONSTRAINT uq_cert_provider_code UNIQUE (provider, source_code);

ALTER TABLE hobbies
    ADD COLUMN source VARCHAR(32) NOT NULL DEFAULT 'LEGACY',
    ADD COLUMN active BIT NOT NULL DEFAULT b'1';

ALTER TABLE player_hobbies
    MODIFY COLUMN hobby_id BIGINT NULL,
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'CATALOG',
    ADD COLUMN private_name_key VARCHAR(60) COLLATE utf8mb4_bin NULL,
    ADD CONSTRAINT uq_player_private_hobby UNIQUE (player_id, private_name_key);

INSERT INTO hobbies (created_at, updated_at, name, category, source, active)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), recommendations.name, recommendations.category, 'SERVICE_CURATED', b'1'
FROM (
    SELECT '독서' name, 'READING' category UNION ALL
    SELECT '글쓰기', 'WRITING' UNION ALL SELECT '영화 감상', 'ARTS' UNION ALL
    SELECT '음악 감상', 'MUSIC' UNION ALL SELECT '기타 연주', 'MUSIC' UNION ALL
    SELECT '피아노 연주', 'MUSIC' UNION ALL SELECT '사진 촬영', 'PHOTOGRAPHY' UNION ALL
    SELECT '그림 그리기', 'ARTS' UNION ALL SELECT '요리', 'COOKING' UNION ALL
    SELECT '베이킹', 'BAKING' UNION ALL SELECT '걷기', 'FITNESS' UNION ALL
    SELECT '달리기', 'FITNESS' UNION ALL SELECT '등산', 'OUTDOORS' UNION ALL
    SELECT '수영', 'SPORTS' UNION ALL SELECT '자전거', 'SPORTS' UNION ALL
    SELECT '요가', 'WELLNESS' UNION ALL SELECT '보드게임', 'BOARD_GAMES' UNION ALL
    SELECT '뜨개질', 'CRAFTS'
) recommendations
WHERE NOT EXISTS (SELECT 1 FROM hobbies existing WHERE existing.name = recommendations.name AND existing.category = recommendations.category);
