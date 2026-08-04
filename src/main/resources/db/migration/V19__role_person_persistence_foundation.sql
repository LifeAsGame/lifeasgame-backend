CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    role_type VARCHAR(40) NOT NULL,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_id_player UNIQUE (id, player_id),
    CONSTRAINT ck_role_player CHECK (player_id > 0),
    CONSTRAINT ck_role_type CHECK (
        CHAR_LENGTH(TRIM(role_type)) BETWEEN 1 AND 40
    ),
    CONSTRAINT ck_role_name CHECK (
        CHAR_LENGTH(TRIM(name)) BETWEEN 1 AND 60
    ),
    CONSTRAINT ck_role_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    INDEX idx_role_player_status (player_id, status, id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE persons (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_player_id BIGINT NOT NULL,
    linked_user_id BIGINT NULL,
    display_name VARCHAR(80) NOT NULL,
    notes TEXT NULL,
    birthday DATE NULL,
    contact VARCHAR(120) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_person_id_owner UNIQUE (id, owner_player_id),
    CONSTRAINT uq_person_owner_linked_user UNIQUE (owner_player_id, linked_user_id),
    CONSTRAINT ck_person_owner CHECK (owner_player_id > 0),
    CONSTRAINT ck_person_linked_user CHECK (
        linked_user_id IS NULL OR linked_user_id > 0
    ),
    CONSTRAINT ck_person_display_name CHECK (
        CHAR_LENGTH(TRIM(display_name)) BETWEEN 1 AND 80
    ),
    CONSTRAINT ck_person_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    INDEX idx_person_owner_status (owner_player_id, status, id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_relations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    relation_type VARCHAR(40) NOT NULL,
    role_notes TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_relation_role_person UNIQUE (role_id, person_id),
    CONSTRAINT ck_role_relation_player CHECK (player_id > 0),
    CONSTRAINT ck_role_relation_role CHECK (role_id > 0),
    CONSTRAINT ck_role_relation_person CHECK (person_id > 0),
    CONSTRAINT ck_role_relation_type CHECK (
        CHAR_LENGTH(TRIM(relation_type)) BETWEEN 1 AND 40
    ),
    CONSTRAINT ck_role_relation_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    INDEX idx_role_relation_player_role_status (player_id, role_id, status, id),
    INDEX idx_role_relation_player_person_status (player_id, person_id, status, id),
    CONSTRAINT fk_role_relation_role_owner
        FOREIGN KEY (role_id, player_id)
        REFERENCES roles (id, player_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_role_relation_person_owner
        FOREIGN KEY (person_id, player_id)
        REFERENCES persons (id, owner_player_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
