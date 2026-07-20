package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_migration")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Test
    void cleanDatabaseMigratesToVersionOne() throws Exception {
        Flyway flyway = flyway();

        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(appliedVersions()).containsExactly("1");
        assertThat(existingTables("users", "player", "quests", "items", "inventory_entries"))
                .containsExactlyInAnyOrder("users", "player", "quests", "items", "inventory_entries");
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
            Set<String> versions = new java.util.LinkedHashSet<>();
            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
            return versions;
        }
    }

    private Set<String> existingTables(String... tableNames) throws Exception {
        String placeholders = String.join(", ", java.util.Collections.nCopies(tableNames.length, "?"));
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
                Set<String> tables = new java.util.LinkedHashSet<>();
                while (resultSet.next()) {
                    tables.add(resultSet.getString("table_name"));
                }
                return tables;
            }
        }
    }
}
