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
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        @DisplayName("V1과 V2가 적용되고 Reward seed profile을 조회할 수 있다")
        void migratesSchemaAndSeedsRewardProfiles() throws Exception {
            Flyway flyway = flyway();

            MigrateResult result = flyway.migrate();

            assertThat(result.migrationsExecuted).isEqualTo(2);
            assertThat(appliedVersions()).containsExactly("1", "2");
            assertThat(existingTables(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines"
            )).containsExactlyInAnyOrder(
                    "users",
                    "player",
                    "quests",
                    "items",
                    "inventory_entries",
                    "reward_definitions",
                    "reward_profiles",
                    "reward_profile_lines"
            );
            assertThat(seedProfileCodes()).containsExactly("RP_EXP_10", "RP_EXP_30");
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
}
