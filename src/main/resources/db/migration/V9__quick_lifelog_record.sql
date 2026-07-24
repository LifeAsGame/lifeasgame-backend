CREATE TABLE quick_record_request_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    source_type VARCHAR(20) NULL,
    source_id BIGINT NULL,
    recorded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quick_record_request_receipt_identity
        UNIQUE (player_id, idempotency_key),
    CONSTRAINT ck_quick_record_request_receipt_player
        CHECK (player_id > 0),
    CONSTRAINT ck_quick_record_request_receipt_key
        CHECK (CHAR_LENGTH(TRIM(idempotency_key)) > 0),
    CONSTRAINT ck_quick_record_request_receipt_result CHECK (
        (
            source_type IS NULL
            AND source_id IS NULL
            AND recorded_at IS NULL
        )
        OR
        (
            source_type IS NOT NULL
            AND source_id IS NOT NULL
            AND recorded_at IS NOT NULL
        )
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
