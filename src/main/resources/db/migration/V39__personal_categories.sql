CREATE TABLE personal_categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_player_id BIGINT NOT NULL,
    kind VARCHAR(20) NOT NULL,
    name VARCHAR(80) NOT NULL,
    normalized_name VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
    CONSTRAINT uq_personal_category_name UNIQUE (owner_player_id, kind, normalized_name),
    CONSTRAINT fk_personal_category_owner FOREIGN KEY (owner_player_id) REFERENCES player (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE player_certifications
    ADD COLUMN personal_category_id BIGINT NULL,
    ADD INDEX idx_player_cert_personal_category (personal_category_id),
    ADD CONSTRAINT fk_player_cert_personal_category FOREIGN KEY (personal_category_id)
        REFERENCES personal_categories (id) ON DELETE SET NULL;

ALTER TABLE player_hobbies
    ADD COLUMN personal_category_id BIGINT NULL,
    ADD INDEX idx_player_hobby_personal_category (personal_category_id),
    ADD CONSTRAINT fk_player_hobby_personal_category FOREIGN KEY (personal_category_id)
        REFERENCES personal_categories (id) ON DELETE SET NULL;
