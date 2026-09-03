CREATE TABLE marketplace_purchase_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    buyer_player_id BIGINT NOT NULL,
    idempotency_key VARCHAR(128)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    request_fingerprint CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    trade_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_marketplace_purchase_receipt_identity
        UNIQUE (buyer_player_id, idempotency_key),
    CONSTRAINT ck_marketplace_purchase_receipt_buyer
        CHECK (buyer_player_id > 0),
    CONSTRAINT ck_marketplace_purchase_receipt_key
        CHECK (CHAR_LENGTH(TRIM(idempotency_key)) > 0),
    CONSTRAINT fk_marketplace_purchase_receipt_trade
        FOREIGN KEY (trade_id) REFERENCES trades (id),
    INDEX idx_marketplace_purchase_receipt_trade (trade_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
