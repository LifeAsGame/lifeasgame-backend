CREATE TABLE personal_role_group_links (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_player_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    group_type VARCHAR(20) NOT NULL,
    group_id BIGINT NOT NULL,
    CONSTRAINT uq_personal_role_group UNIQUE (owner_player_id, role_id, group_type, group_id),
    INDEX idx_personal_role_group_page (owner_player_id, role_id, id),
    CONSTRAINT fk_personal_role_group_owner FOREIGN KEY (role_id, owner_player_id)
        REFERENCES roles (id, player_id) ON DELETE RESTRICT,
    CONSTRAINT ck_personal_role_group_type CHECK (group_type IN ('GUILD', 'PARTY', 'ROLE_PARTY')),
    CONSTRAINT ck_personal_role_group_id CHECK (group_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
