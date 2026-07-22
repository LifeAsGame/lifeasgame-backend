CREATE TABLE player_growth_changes (
    before_level INTEGER NOT NULL,
    after_level INTEGER NOT NULL,
    applied_exp BIGINT NOT NULL,
    before_total_exp BIGINT NOT NULL,
    after_total_exp BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    leftover_exp BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    requested_exp BIGINT NOT NULL,
    reward_line_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_player_growth_change_reward_line UNIQUE (reward_line_id),
    CONSTRAINT ck_player_growth_change_player CHECK (player_id > 0),
    CONSTRAINT ck_player_growth_change_reward_line CHECK (reward_line_id > 0),
    CONSTRAINT ck_player_growth_change_requested_exp CHECK (requested_exp > 0),
    CONSTRAINT ck_player_growth_change_applied_exp CHECK (applied_exp >= 0),
    CONSTRAINT ck_player_growth_change_leftover_exp CHECK (leftover_exp >= 0),
    CONSTRAINT ck_player_growth_change_exp_sum CHECK (requested_exp = applied_exp + leftover_exp),
    CONSTRAINT ck_player_growth_change_levels CHECK (
        before_level > 0 AND after_level >= before_level
    ),
    CONSTRAINT ck_player_growth_change_totals CHECK (
        before_total_exp >= 0
        AND after_total_exp >= before_total_exp
        AND after_total_exp - before_total_exp = applied_exp
    ),
    CONSTRAINT fk_player_growth_change_player
        FOREIGN KEY (player_id) REFERENCES player (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_player_growth_change_player
    ON player_growth_changes (player_id);
