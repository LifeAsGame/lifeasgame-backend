CREATE TABLE private_guild_member_notes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_player_id BIGINT NOT NULL,
    guild_id BIGINT NOT NULL,
    target_member_player_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    note_text VARCHAR(2000) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_private_guild_member_note UNIQUE (author_player_id, guild_id, target_member_player_id),
    INDEX idx_private_guild_note_person_page (author_player_id, person_id, id),
    CONSTRAINT fk_private_guild_note_person FOREIGN KEY (person_id, author_player_id)
        REFERENCES persons (id, owner_player_id) ON DELETE RESTRICT,
    CONSTRAINT fk_private_guild_note_guild FOREIGN KEY (guild_id)
        REFERENCES guilds (guild_id) ON DELETE RESTRICT,
    CONSTRAINT ck_private_guild_note_target CHECK (target_member_player_id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
