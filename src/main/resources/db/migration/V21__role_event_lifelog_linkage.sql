CREATE TABLE role_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    starts_at DATETIME(6) NULL,
    ends_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_event_id_role_player
        UNIQUE (id, role_id, player_id),
    CONSTRAINT ck_role_event_player CHECK (player_id > 0),
    CONSTRAINT ck_role_event_role CHECK (role_id > 0),
    CONSTRAINT ck_role_event_title CHECK (
        CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 120
    ),
    CONSTRAINT ck_role_event_time_range CHECK (
        starts_at IS NULL OR ends_at IS NULL OR ends_at >= starts_at
    ),
    CONSTRAINT ck_role_event_status CHECK (
        status IN ('PLANNED', 'COMPLETED', 'CANCELED')
    ),
    CONSTRAINT ck_role_event_completion CHECK (
        (status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (status IN ('PLANNED', 'CANCELED') AND completed_at IS NULL)
    ),
    INDEX idx_role_event_player_role_status (
        player_id, role_id, status, id
    ),
    CONSTRAINT fk_role_event_role_owner
        FOREIGN KEY (role_id, player_id)
        REFERENCES roles (id, player_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE role_event_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role_event_id BIGINT NOT NULL,
    participant_type VARCHAR(20) NOT NULL,
    participant_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_event_participant UNIQUE (
        role_event_id, participant_type, participant_id
    ),
    CONSTRAINT ck_role_event_participant_type CHECK (
        participant_type IN ('PERSON', 'SERVICE_USER')
    ),
    CONSTRAINT ck_role_event_participant_id CHECK (participant_id > 0),
    CONSTRAINT fk_role_event_participant_event
        FOREIGN KEY (role_event_id)
        REFERENCES role_events (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE life_log_records
    DROP CHECK ck_life_log_record_primary_role,
    ADD COLUMN role_event_id BIGINT NULL AFTER primary_role_id,
    ADD CONSTRAINT ck_life_log_record_primary_role
        CHECK (primary_role_id IS NULL OR primary_role_id > 0),
    ADD CONSTRAINT ck_life_log_record_role_event
        CHECK (
            role_event_id IS NULL
            OR (role_event_id > 0 AND primary_role_id IS NOT NULL)
        ),
    ADD INDEX idx_life_log_record_player_role_event (
        player_id, primary_role_id, role_event_id, id
    ),
    ADD CONSTRAINT fk_life_log_record_role_owner
        FOREIGN KEY (primary_role_id, player_id)
        REFERENCES roles (id, player_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_life_log_record_role_event_context
        FOREIGN KEY (role_event_id, primary_role_id, player_id)
        REFERENCES role_events (id, role_id, player_id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT;
