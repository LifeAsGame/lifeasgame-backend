CREATE TABLE inventory_reward_deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reward_line_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    item_code VARCHAR(80) NOT NULL,
    item_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    delivered_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_inventory_reward_delivery_line UNIQUE (reward_line_id),
    CONSTRAINT ck_inventory_reward_delivery_line CHECK (reward_line_id > 0),
    CONSTRAINT ck_inventory_reward_delivery_player CHECK (player_id > 0),
    CONSTRAINT ck_inventory_reward_delivery_item CHECK (item_id > 0),
    CONSTRAINT ck_inventory_reward_delivery_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reward_delivery_item_code CHECK (
        CHAR_LENGTH(TRIM(item_code)) BETWEEN 1 AND 80
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_inventory_reward_delivery_player
    ON inventory_reward_deliveries (player_id, delivered_at);
