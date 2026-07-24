CREATE TABLE quest_signal_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quest_code VARCHAR(80) NOT NULL,
    player_id BIGINT NOT NULL,
    correlation_id VARCHAR(120) NOT NULL,
    signal_type VARCHAR(30) NOT NULL,
    payload_fingerprint CHAR(64) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_quest_signal_receipt_identity
        UNIQUE (quest_code, player_id, correlation_id),
    INDEX idx_quest_signal_receipt_player (player_id),
    INDEX idx_quest_signal_receipt_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
