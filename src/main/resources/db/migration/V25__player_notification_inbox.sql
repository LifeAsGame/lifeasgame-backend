CREATE TABLE player_notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    source_event_id VARCHAR(255) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_player_notification_source
        UNIQUE (player_id, source_event_id),
    CONSTRAINT ck_player_notification_player CHECK (player_id > 0),
    CONSTRAINT ck_player_notification_type CHECK (type IN (
        'MAIL_RECEIVED',
        'QUEST_PROGRESS',
        'QUEST_COMPLETED',
        'QUEST_REWARD_READY',
        'LISTING_SOLD',
        'ACHIEVEMENT_UNLOCK',
        'SYSTEM_NOTICE'
    )),
    CONSTRAINT ck_player_notification_source_event CHECK (
        CHAR_LENGTH(TRIM(source_event_id)) BETWEEN 1 AND 255
    ),
    CONSTRAINT ck_player_notification_title CHECK (
        CHAR_LENGTH(TRIM(title)) BETWEEN 1 AND 200
    ),
    CONSTRAINT ck_player_notification_body CHECK (
        CHAR_LENGTH(TRIM(body)) >= 1
    ),
    INDEX idx_player_notification_inbox (player_id, id),
    INDEX idx_player_notification_unread (player_id, read_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
