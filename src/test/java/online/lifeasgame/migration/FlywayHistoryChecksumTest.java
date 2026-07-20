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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayHistoryChecksumTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_history")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Test
    void secondMigrationIsNoOpAndKeepsVersionOneHistory() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();

        MigrateResult first = flyway.migrate();
        HistoryRow firstHistory = versionOneHistory();

        MigrateResult second = flyway.migrate();
        HistoryRow secondHistory = versionOneHistory();

        assertThat(first.migrationsExecuted).isEqualTo(1);
        assertThat(second.migrationsExecuted).isZero();
        assertThat(secondHistory).isEqualTo(firstHistory);
        assertThat(secondHistory.version()).isEqualTo("1");
        assertThat(secondHistory.checksum()).isNotNull();
        assertThat(secondHistory.success()).isTrue();
        assertThat(successfulVersionOneRows()).isEqualTo(1);
    }

    private HistoryRow versionOneHistory() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT installed_rank, version, description, type, script, checksum, success
                     FROM flyway_schema_history
                     WHERE version = '1'
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new HistoryRow(
                    resultSet.getInt("installed_rank"),
                    resultSet.getString("version"),
                    resultSet.getString("description"),
                    resultSet.getString("type"),
                    resultSet.getString("script"),
                    resultSet.getObject("checksum", Integer.class),
                    resultSet.getBoolean("success")
            );
        }
    }

    private int successfulVersionOneRows() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM flyway_schema_history
                     WHERE version = '1' AND success = TRUE
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt(1);
        }
    }

    private record HistoryRow(
            int installedRank,
            String version,
            String description,
            String type,
            String script,
            Integer checksum,
            boolean success
    ) {
    }
}
