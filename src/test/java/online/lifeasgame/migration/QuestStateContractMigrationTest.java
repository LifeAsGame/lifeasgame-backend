package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("V6 Quest 상태 계약 migration")
class QuestStateContractMigrationTest {

    private static final LocalDateTime LEGACY_COMPLETED_AT =
            LocalDateTime.of(2026, 7, 23, 12, 34, 56, 123_456_000);

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_quest_state_contract")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Nested
    @DisplayName("V5 schema의 기존 Quest 데이터를 V6로 올릴 때")
    class MigrateLegacyDone {

        @Test
        @DisplayName("DONE을 COMPLETED로 이관하고 timestamp와 AUTO 정책을 보존한다")
        void migratesDoneToCompleted() throws Exception {
            Flyway v5Flyway = flyway(MigrationVersion.fromVersion("5"));
            v5Flyway.clean();
            assertThat(v5Flyway.migrate().migrationsExecuted).isEqualTo(5);
            insertLegacyQuestAndDoneAcceptance();

            MigrateResult result = flyway(
                    MigrationVersion.fromVersion("6")
            ).migrate();

            assertThat(result.migrationsExecuted).isEqualTo(1);
            assertThat(result.targetSchemaVersion).isEqualTo("6");
            assertThat(legacyAcceptance()).isEqualTo(
                    new AcceptanceRow(
                            "COMPLETED",
                            LEGACY_COMPLETED_AT,
                            LEGACY_COMPLETED_AT
                    )
            );
            assertThat(existingQuestCompletionPolicy()).isEqualTo("AUTO");
            assertThat(questCompletionPolicyColumn())
                    .contains("'AUTO'", "'USER_CONFIRM'");
            assertThat(acceptanceStatusColumn())
                    .contains(
                            "'IN_PROGRESS'",
                            "'GOAL_REACHED'",
                            "'COMPLETED'",
                            "'CANCELED'"
                    )
                    .doesNotContain("'DONE'");
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void insertLegacyQuestAndDoneAcceptance() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO quests (
                        id,
                        reward_exp,
                        target_value,
                        created_at,
                        updated_at,
                        code,
                        title_id,
                        reward_stats,
                        category,
                        description_md,
                        repeat_rule,
                        target_type
                    ) VALUES (
                        193,
                        0,
                        1,
                        '2026-07-23 12:00:00.000000',
                        '2026-07-23 12:00:00.000000',
                        'quest:test:legacy-done',
                        'Legacy DONE Quest',
                        JSON_OBJECT(),
                        'MAIN',
                        'V6 migration test',
                        'NONE',
                        'COUNT'
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO quest_acceptances (
                        id,
                        period_start,
                        period_end,
                        progress_value,
                        completed_at,
                        created_at,
                        updated_at,
                        player_id,
                        quest_id,
                        version,
                        status
                    ) VALUES (
                        1930,
                        '2026-07-23',
                        '2026-07-23',
                        1,
                        '2026-07-23 12:34:56.123456',
                        '2026-07-23 12:00:00.000000',
                        '2026-07-23 12:34:56.123456',
                        19300,
                        193,
                        0,
                        'DONE'
                    )
                    """);
        }
    }

    private AcceptanceRow legacyAcceptance() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT status, goal_reached_at, completed_at
                     FROM quest_acceptances
                     WHERE id = 1930
                     """)) {
            assertThat(resultSet.next()).isTrue();
            AcceptanceRow result = new AcceptanceRow(
                    resultSet.getString("status"),
                    resultSet.getTimestamp("goal_reached_at").toLocalDateTime(),
                    resultSet.getTimestamp("completed_at").toLocalDateTime()
            );
            assertThat(resultSet.next()).isFalse();
            return result;
        }
    }

    private String existingQuestCompletionPolicy() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT completion_policy
                     FROM quests
                     WHERE id = 193
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private String questCompletionPolicyColumn() throws Exception {
        return columnType("quests", "completion_policy");
    }

    private String acceptanceStatusColumn() throws Exception {
        return columnType("quest_acceptances", "status");
    }

    private String columnType(String tableName, String columnName) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             var statement = connection.prepareStatement("""
                     SELECT column_type
                     FROM information_schema.columns
                     WHERE table_schema = DATABASE()
                       AND table_name = ?
                       AND column_name = ?
                     """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getString(1);
            }
        }
    }

    private record AcceptanceRow(
            String status,
            LocalDateTime goalReachedAt,
            LocalDateTime completedAt
    ) {
    }
}
