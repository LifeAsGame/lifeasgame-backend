CREATE TABLE IF NOT EXISTS achievement_award_receipts (
    player_id BIGINT NOT NULL,
    achievement_code VARCHAR(60) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    status VARCHAR(24) NOT NULL,
    linked_title_code VARCHAR(60),
    PRIMARY KEY (player_id, achievement_code)
);
