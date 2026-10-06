CREATE TABLE lifelog_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_player_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    system_code VARCHAR(20) COLLATE utf8mb4_bin NULL,
    name VARCHAR(80) NULL,
    normalized_name VARCHAR(80) COLLATE utf8mb4_bin NULL,
    hidden BIT NOT NULL DEFAULT b'0',
    CONSTRAINT uq_lifelog_system_category UNIQUE (owner_player_id, kind, system_code),
    CONSTRAINT uq_lifelog_personal_category UNIQUE (owner_player_id, kind, normalized_name),
    CONSTRAINT ck_lifelog_category_source CHECK (
        (source = 'SYSTEM' AND system_code IS NOT NULL AND name IS NULL AND normalized_name IS NULL)
        OR (source = 'PERSONAL' AND system_code IS NULL AND name IS NOT NULL AND normalized_name IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO lifelog_categories (owner_player_id, kind, source, system_code, hidden)
SELECT DISTINCT player_id, 'COLLECTION', 'SYSTEM', CAST(category AS CHAR), b'0' FROM collection_logs;
INSERT INTO lifelog_categories (owner_player_id, kind, source, system_code, hidden)
SELECT DISTINCT player_id, 'EXERCISE', 'SYSTEM', CAST(category AS CHAR), b'0' FROM exercise_logs;
INSERT INTO lifelog_categories (owner_player_id, kind, source, system_code, hidden)
SELECT DISTINCT player_id, 'MEDIA', 'SYSTEM', CAST(category AS CHAR), b'0' FROM media_logs;

ALTER TABLE collection_logs ADD COLUMN personal_category_id BIGINT NULL,
    ADD INDEX idx_collection_personal_category (player_id, personal_category_id, id),
    ADD CONSTRAINT fk_collection_lifelog_category FOREIGN KEY (personal_category_id)
        REFERENCES lifelog_categories (id) ON DELETE SET NULL;
ALTER TABLE exercise_logs ADD COLUMN personal_category_id BIGINT NULL,
    ADD INDEX idx_exercise_personal_category (player_id, personal_category_id, id),
    ADD CONSTRAINT fk_exercise_lifelog_category FOREIGN KEY (personal_category_id)
        REFERENCES lifelog_categories (id) ON DELETE SET NULL;
ALTER TABLE media_logs ADD COLUMN personal_category_id BIGINT NULL,
    ADD INDEX idx_media_personal_category (player_id, personal_category_id, id),
    ADD CONSTRAINT fk_media_lifelog_category FOREIGN KEY (personal_category_id)
        REFERENCES lifelog_categories (id) ON DELETE SET NULL;
