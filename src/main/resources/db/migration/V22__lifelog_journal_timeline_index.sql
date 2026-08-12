CREATE INDEX idx_life_log_record_player_timeline
    ON life_log_records (player_id, occurred_at DESC, id DESC);
