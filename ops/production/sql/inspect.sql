-- READ ONLY. Run only through the separately approved private route.
-- No application startup, migrate, repair, baseline, DDL, or business rows.
SET SESSION MAX_EXECUTION_TIME=10000;
START TRANSACTION READ ONLY;
SELECT VERSION() AS engine_version, DATABASE() AS database_name,
       @@GLOBAL.read_only AS server_read_only, @@GLOBAL.event_scheduler AS event_scheduler;
SELECT ENGINE, COUNT(*) AS table_count,
       SUM(DATA_LENGTH + INDEX_LENGTH) AS estimated_bytes
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE'
GROUP BY ENGINE;
SELECT TABLE_NAME, ENGINE, TABLE_ROWS AS estimated_rows,
       DATA_LENGTH + INDEX_LENGTH AS estimated_bytes
FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME;
SELECT COUNT(*) AS history_table_exists FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='flyway_schema_history';
SELECT COUNT(*) AS routine_count FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA=DATABASE();
SELECT COUNT(*) AS trigger_count FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE();
SELECT EVENT_NAME, STATUS, EVENT_TYPE FROM information_schema.EVENTS WHERE EVENT_SCHEMA=DATABASE();
COMMIT;
-- Execute production-flyway-history.sql only if history_table_exists=1.
