CREATE TABLE guild_group_links (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    group_type VARCHAR(20) NOT NULL,
    group_id BIGINT NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    proposed_by_player_id BIGINT NOT NULL,
    guild_approved_by_player_id BIGINT NULL,
    group_approved_by_player_id BIGINT NULL,
    open_slot INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_guild_group_link_guild FOREIGN KEY (guild_id) REFERENCES guilds (guild_id) ON DELETE RESTRICT,
    CONSTRAINT fk_guild_group_link_proposer FOREIGN KEY (proposed_by_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT uq_guild_group_open UNIQUE (guild_id, group_type, group_id, open_slot),
    CONSTRAINT ck_guild_group_type CHECK (group_type IN ('PARTY', 'ROLE_PARTY')),
    CONSTRAINT ck_guild_group_status CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'CANCELED', 'UNLINKED')),
    CONSTRAINT ck_guild_group_open_slot CHECK ((status IN ('PENDING', 'ACTIVE') AND open_slot = 1) OR (status NOT IN ('PENDING', 'ACTIVE') AND open_slot IS NULL)),
    CONSTRAINT ck_guild_group_name CHECK (CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 120),
    INDEX idx_guild_group_list (guild_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guild_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    guild_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    shared_description VARCHAR(2000) NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    location VARCHAR(200) NULL,
    status VARCHAR(20) NOT NULL,
    created_by_player_id BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_guild_event_guild FOREIGN KEY (guild_id) REFERENCES guilds (guild_id) ON DELETE RESTRICT,
    CONSTRAINT fk_guild_event_creator FOREIGN KEY (created_by_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT ck_guild_event_time CHECK (ends_at > starts_at),
    CONSTRAINT ck_guild_event_status CHECK (status IN ('PLANNED', 'COMPLETED', 'CANCELED')),
    INDEX idx_guild_event_list (guild_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE guild_event_rsvps (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    guild_event_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_guild_event_rsvp UNIQUE (guild_event_id, player_id),
    CONSTRAINT fk_guild_event_rsvp_event FOREIGN KEY (guild_event_id) REFERENCES guild_events (id) ON DELETE RESTRICT,
    CONSTRAINT fk_guild_event_rsvp_player FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE RESTRICT,
    INDEX idx_guild_event_rsvp_active (guild_event_id, active, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
