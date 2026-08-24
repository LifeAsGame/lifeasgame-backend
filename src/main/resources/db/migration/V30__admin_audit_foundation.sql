CREATE TABLE admin_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    reason VARCHAR(512),
    result VARCHAR(16) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(128),
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_audit_actor
        FOREIGN KEY (actor_user_id) REFERENCES users (id),
    CONSTRAINT ck_admin_audit_action
        CHECK (action REGEXP '^[A-Z][A-Z0-9_]{2,63}$'),
    CONSTRAINT ck_admin_audit_target_type
        CHECK (target_type REGEXP '^[A-Z][A-Z0-9_]{2,63}$'),
    CONSTRAINT ck_admin_audit_result
        CHECK (result IN ('SUCCESS', 'FAILED')),
    INDEX idx_admin_audit_actor_time
        (actor_user_id, occurred_at DESC, id DESC),
    INDEX idx_admin_audit_target_time
        (target_type, target_id, occurred_at DESC, id DESC),
    INDEX idx_admin_audit_action_time
        (action, occurred_at DESC, id DESC),
    INDEX idx_admin_audit_result_time
        (result, occurred_at DESC, id DESC),
    INDEX idx_admin_audit_correlation (correlation_id),
    INDEX idx_admin_audit_time (occurred_at DESC, id DESC)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
