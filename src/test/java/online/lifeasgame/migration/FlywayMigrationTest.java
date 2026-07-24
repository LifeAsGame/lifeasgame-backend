package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("Flyway migration")
class FlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_migration")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Nested
    @DisplayName("빈 MySQL 8 데이터베이스에 migration을 적용할 때")
    class MigrateCleanDatabase {

        @Test
        @DisplayName("V1부터 V9까지 적용되고 Quick Record Receipt unique가 생성된다")
        void migratesSchemaAndSeedsRewardProfiles() throws Exception {
            Flyway flyway = flyway();

            MigrateResult result = flyway.migrate();

            assertThat(result.migrationsExecuted).isEqualTo(9);
            assertThat(appliedVersions())
                    .containsExactly(
                            "1", "2", "3", "4", "5",
                            "6", "7", "8", "9"
                    );
            assertThat(existingTables(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines",
                    "reward_settlements",
                    "reward_settlement_lines",
                    "player_growth_changes",
                    "quest_signal_receipts",
                    "outbox_events",
                    "quick_record_request_receipts"
            )).containsExactlyInAnyOrder(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines",
                    "reward_settlements",
                    "reward_settlement_lines",
                    "player_growth_changes",
                    "quest_signal_receipts",
                    "outbox_events",
                    "quick_record_request_receipts"
            );
            assertThat(existingTables(
                    "quick_lifelog_entries",
                    "quick_lifelog_entry_tags",
                    "quick_lifelog_request_receipts"
            )).isEmpty();
            assertThat(seedProfileCodes()).containsExactly("RP_EXP_10", "RP_EXP_30");
            assertThat(noRewardProfile()).isEqualTo(new NoRewardProfile("ACTIVE", 0));
            assertThat(uniqueIndexColumns("reward_settlements", "uq_reward_settlement_source"))
                    .containsExactly("player_id", "source_type", "source_id");
            assertThat(uniqueIndexColumns(
                    "reward_settlement_lines",
                    "uq_reward_settlement_line_sort_order"
            )).containsExactly("reward_settlement_id", "sort_order");
            assertThat(uniqueIndexColumns(
                    "player_growth_changes",
                    "uq_player_growth_change_reward_line"
            )).containsExactly("reward_line_id");
            assertThat(uniqueIndexColumns(
                    "quest_signal_receipts",
                    "uq_quest_signal_receipt_identity"
            )).containsExactly("quest_code", "player_id", "correlation_id");
            assertThat(indexColumns(
                    "quest_signal_receipts",
                    "idx_quest_signal_receipt_player"
            )).containsExactly("player_id");
            assertThat(indexColumns(
                    "quest_signal_receipts",
                    "idx_quest_signal_receipt_created_at"
            )).containsExactly("created_at");
            assertThat(uniqueIndexColumns(
                    "outbox_events",
                    "uq_outbox_event_event_id"
            )).containsExactly("event_id");
            assertThat(indexColumns(
                    "outbox_events",
                    "idx_outbox_event_ready"
            )).containsExactly("status", "next_attempt_at", "id");
            assertThat(indexColumns(
                    "outbox_events",
                    "idx_outbox_event_lease"
            )).containsExactly("status", "locked_at");
            assertThat(uniqueIndexColumns(
                    "quick_record_request_receipts",
                    "uq_quick_record_request_receipt_identity"
            )).containsExactly("player_id", "idempotency_key");
            insertSettlementIdentity();
            assertThatThrownBy(FlywayMigrationTest.this::insertSettlementIdentity)
                    .isInstanceOfSatisfying(SQLException.class, exception ->
                            assertThat(exception.getSQLState()).startsWith("23")
                    );
        }
    }

    private Flyway flyway() {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
    }

    private Set<String> appliedVersions() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT version
                     FROM flyway_schema_history
                     WHERE success = TRUE
                     ORDER BY installed_rank
                     """)) {
            Set<String> versions = new LinkedHashSet<>();
            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
            return versions;
        }
    }

    private Set<String> existingTables(String... tableNames) throws Exception {
        String placeholders = String.join(", ", Collections.nCopies(tableNames.length, "?"));
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (%s)
                """.formatted(placeholders);

        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < tableNames.length; index++) {
                statement.setString(index + 1, tableNames[index]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                Set<String> tables = new LinkedHashSet<>();
                while (resultSet.next()) {
                    tables.add(resultSet.getString("table_name"));
                }
                return tables;
            }
        }
    }

    private Set<String> seedProfileCodes() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT profile.code
                     FROM reward_profiles profile
                     JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                     JOIN reward_definitions definition ON definition.id = line.reward_definition_id
                     WHERE profile.status = 'ACTIVE'
                       AND definition.reward_type = 'EXP'
                     ORDER BY profile.code
                     """)) {
            Set<String> codes = new LinkedHashSet<>();
            while (resultSet.next()) {
                codes.add(resultSet.getString("code"));
            }
            return codes;
        }
    }

    private NoRewardProfile noRewardProfile() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT profile.status, COUNT(line.id) AS line_count
                     FROM reward_profiles profile
                     LEFT JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                     WHERE profile.code = 'RP_NONE'
                     GROUP BY profile.id, profile.status
                     """)) {
            assertThat(resultSet.next()).isTrue();
            NoRewardProfile result = new NoRewardProfile(
                    resultSet.getString("status"),
                    resultSet.getInt("line_count")
            );
            assertThat(resultSet.next()).isFalse();
            return result;
        }
    }

    private List<String> uniqueIndexColumns(String tableName, String indexName) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT column_name
                     FROM information_schema.statistics
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                       AND index_name = ?
                       AND non_unique = 0
                     ORDER BY seq_in_index
                     """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                return columns;
            }
        }
    }

    private List<String> indexColumns(
            String tableName,
            String indexName
    ) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT column_name
                     FROM information_schema.statistics
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                       AND index_name = ?
                     ORDER BY seq_in_index
                     """)) {
            statement.setString(1, tableName);
            statement.setString(2, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<String> columns = new ArrayList<>();
                while (resultSet.next()) {
                    columns.add(resultSet.getString("column_name"));
                }
                return columns;
            }
        }
    }

    private void insertSettlementIdentity() throws SQLException {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO reward_settlements (
                        player_id,
                        source_type,
                        source_id,
                        reward_profile_id,
                        reward_profile_code,
                        status,
                        created_at,
                        updated_at
                    ) VALUES (
                        185,
                        'QUEST_COMPLETION',
                        185001,
                        1,
                        'RP_EXP_10',
                        'PENDING',
                        CURRENT_TIMESTAMP(6),
                        CURRENT_TIMESTAMP(6)
                    )
                    """);
        }
    }

    private record NoRewardProfile(String status, int lineCount) {
    }
}
