CREATE TABLE group_activities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    party_id BIGINT NULL,
    role_party_id BIGINT NULL,
    created_by_player_id BIGINT NOT NULL,
    client_request_id CHAR(36) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    title VARCHAR(120) NOT NULL,
    shared_description VARCHAR(2000) NULL,
    location VARCHAR(200) NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_activity_party FOREIGN KEY (party_id) REFERENCES parties (party_id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_activity_role_party FOREIGN KEY (role_party_id) REFERENCES role_parties (id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_activity_creator FOREIGN KEY (created_by_player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT uq_group_activity_party_request UNIQUE (party_id, created_by_player_id, client_request_id),
    CONSTRAINT uq_group_activity_role_party_request UNIQUE (role_party_id, created_by_player_id, client_request_id),
    CONSTRAINT ck_group_activity_owner CHECK ((party_id IS NULL) <> (role_party_id IS NULL)),
    CONSTRAINT ck_group_activity_time CHECK (ends_at > starts_at),
    CONSTRAINT ck_group_activity_status CHECK (status IN ('PLANNED', 'COMPLETED', 'CANCELED')),
    INDEX idx_group_activity_party_time (party_id, starts_at, id),
    INDEX idx_group_activity_role_party_time (role_party_id, starts_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE group_activity_editors (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    party_id BIGINT NULL,
    role_party_id BIGINT NULL,
    player_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_joined_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_activity_editor_party FOREIGN KEY (party_id) REFERENCES parties (party_id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_activity_editor_role_party FOREIGN KEY (role_party_id) REFERENCES role_parties (id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_activity_editor_player FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT uq_group_activity_editor_party UNIQUE (party_id, player_id),
    CONSTRAINT uq_group_activity_editor_role_party UNIQUE (role_party_id, player_id),
    CONSTRAINT ck_group_activity_editor_owner CHECK ((party_id IS NULL) <> (role_party_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE group_activity_rsvps (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    member_joined_at DATETIME(6) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_activity_rsvp_activity FOREIGN KEY (activity_id) REFERENCES group_activities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_activity_rsvp_player FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT uq_group_activity_rsvp UNIQUE (activity_id, player_id),
    INDEX idx_group_activity_rsvp_active (activity_id, active, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
