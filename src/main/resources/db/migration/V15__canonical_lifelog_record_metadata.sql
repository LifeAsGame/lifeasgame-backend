CREATE TABLE life_log_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    source_definition_version INT NOT NULL,
    subtype VARCHAR(20) NULL,
    entry_mode VARCHAR(20) NOT NULL,
    reflection_scope VARCHAR(30) NULL,
    period_key VARCHAR(20) NULL,
    primary_role_id BIGINT NULL,
    occurred_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_life_log_record_source
        UNIQUE (source_type, source_id),
    CONSTRAINT ck_life_log_record_source_type
        CHECK (source_type IN ('COLLECTION', 'EXERCISE', 'MEDIA')),
    CONSTRAINT ck_life_log_record_definition_version
        CHECK (source_definition_version >= 1),
    CONSTRAINT ck_life_log_record_subtype CHECK (
        subtype IS NULL
        OR subtype IN (
            'QUICK_NOTE',
            'ACTIVITY',
            'STUDY',
            'PROJECT',
            'MEMORY',
            'REFLECTION',
            'MOOD',
            'HEALTH_NOTE'
        )
    ),
    CONSTRAINT ck_life_log_record_entry_mode
        CHECK (entry_mode IN ('FULL', 'QUICK')),
    CONSTRAINT ck_life_log_record_reflection_scope CHECK (
        reflection_scope IS NULL
        OR reflection_scope = 'WEEKLY_LOOKBACK'
    ),
    CONSTRAINT ck_life_log_record_reflection_pairing CHECK (
        (
            reflection_scope IS NULL
            AND period_key IS NULL
        )
        OR
        (
            subtype IS NOT NULL
            AND subtype = 'REFLECTION'
            AND reflection_scope = 'WEEKLY_LOOKBACK'
            AND period_key IS NOT NULL
            AND period_key REGEXP '^[0-9]{4}-W(0[1-9]|[1-4][0-9]|5[0-3])$'
        )
    ),
    CONSTRAINT ck_life_log_record_non_reflection_metadata CHECK (
        (
            subtype IS NOT NULL
            AND subtype = 'REFLECTION'
        )
        OR (
            reflection_scope IS NULL
            AND period_key IS NULL
        )
    ),
    CONSTRAINT ck_life_log_record_primary_role
        CHECK (primary_role_id IS NULL)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
