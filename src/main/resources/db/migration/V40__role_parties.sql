CREATE TABLE role_parties (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    creator_player_id BIGINT NOT NULL,
    leader_player_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL,
    max_members INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_role_party_creator_role (creator_player_id, role_id, id),
    CONSTRAINT fk_role_party_role_owner FOREIGN KEY (role_id, creator_player_id)
        REFERENCES roles (id, player_id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_party_leader FOREIGN KEY (leader_player_id)
        REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT ck_role_party_name CHECK (CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 120),
    CONSTRAINT ck_role_party_status CHECK (status IN ('ACTIVE', 'DISBANDED')),
    CONSTRAINT ck_role_party_capacity CHECK (max_members BETWEEN 2 AND 50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_party_members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_party_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    left_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_role_party_member UNIQUE (role_party_id, player_id),
    INDEX idx_role_party_member_player (player_id, role_party_id),
    CONSTRAINT fk_role_party_member_party FOREIGN KEY (role_party_id)
        REFERENCES role_parties (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_party_member_player FOREIGN KEY (player_id)
        REFERENCES player (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_party_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_party_id BIGINT NOT NULL,
    inviter_player_id BIGINT NOT NULL,
    invitee_player_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_role_party_invitee UNIQUE (role_party_id, invitee_player_id),
    INDEX idx_role_party_invitation_mine (invitee_player_id, status, expires_at),
    CONSTRAINT fk_role_party_invitation_party FOREIGN KEY (role_party_id)
        REFERENCES role_parties (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_party_invitation_inviter FOREIGN KEY (inviter_player_id)
        REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT fk_role_party_invitation_invitee FOREIGN KEY (invitee_player_id)
        REFERENCES player (id) ON DELETE RESTRICT,
    CONSTRAINT ck_role_party_invitation_status CHECK (status IN ('PENDING','ACCEPTED','DECLINED','CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
