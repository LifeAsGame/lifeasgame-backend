CREATE TABLE guild_group_creation_receipts (
    guild_id BIGINT NOT NULL,
    actor_player_id BIGINT NOT NULL,
    client_request_id CHAR(36) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    group_type VARCHAR(20) NOT NULL,
    group_id BIGINT NOT NULL,
    link_id BIGINT NOT NULL,
    link_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (guild_id, actor_player_id, client_request_id),
    CONSTRAINT fk_guild_group_receipt_guild FOREIGN KEY (guild_id) REFERENCES guilds (guild_id) ON DELETE RESTRICT,
    CONSTRAINT fk_guild_group_receipt_actor FOREIGN KEY (actor_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT fk_guild_group_receipt_link FOREIGN KEY (link_id) REFERENCES guild_group_links (id) ON DELETE RESTRICT,
    CONSTRAINT ck_guild_group_receipt_type CHECK (group_type IN ('PARTY', 'ROLE_PARTY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE group_roster_entries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    guild_id BIGINT NULL,
    party_id BIGINT NULL,
    display_name VARCHAR(80) NOT NULL,
    group_role_label VARCHAR(120) NULL,
    linked_player_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    active_linked_player_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN linked_player_id ELSE NULL END) STORED,
    CONSTRAINT fk_roster_guild FOREIGN KEY (guild_id) REFERENCES guilds (guild_id) ON DELETE RESTRICT,
    CONSTRAINT fk_roster_party FOREIGN KEY (party_id) REFERENCES parties (party_id) ON DELETE RESTRICT,
    CONSTRAINT fk_roster_linked_player FOREIGN KEY (linked_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT ck_roster_owner CHECK ((guild_id IS NULL) <> (party_id IS NULL)),
    CONSTRAINT ck_roster_status CHECK (status IN ('ACTIVE', 'DELETED')),
    CONSTRAINT uq_roster_guild_player UNIQUE (guild_id, active_linked_player_id),
    CONSTRAINT uq_roster_party_player UNIQUE (party_id, active_linked_player_id),
    INDEX idx_roster_guild_list (guild_id, status, id),
    INDEX idx_roster_party_list (party_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE group_roster_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    roster_entry_id BIGINT NOT NULL,
    target_player_id BIGINT NOT NULL,
    issued_by_player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    pending_entry_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN roster_entry_id ELSE NULL END) STORED,
    CONSTRAINT fk_roster_invitation_entry FOREIGN KEY (roster_entry_id) REFERENCES group_roster_entries (id) ON DELETE RESTRICT,
    CONSTRAINT fk_roster_invitation_target FOREIGN KEY (target_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT fk_roster_invitation_issuer FOREIGN KEY (issued_by_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT ck_roster_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELED', 'EXPIRED')),
    CONSTRAINT uq_roster_pending_entry UNIQUE (pending_entry_id),
    INDEX idx_roster_invitation_target (target_player_id, status, expires_at, id),
    INDEX idx_roster_invitation_entry (roster_entry_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
