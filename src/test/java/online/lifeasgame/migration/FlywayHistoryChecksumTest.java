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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("Flyway migration history")
class FlywayHistoryChecksumTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_history")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Nested
    @DisplayName("migration을 다시 실행할 때")
    class MigrateAgain {

        @Test
        @DisplayName("V1~V13 checksum을 보존하고 V14 이후 두 번째 실행은 no-op이다")
        void keepsMigrationHistory() throws Exception {
            Flyway throughV13 = flyway(MigrationVersion.fromVersion("13"));
            MigrateResult legacy = throughV13.migrate();
            List<HistoryRow> legacyHistory = successfulHistory();
            Flyway flyway = flyway(null);

            MigrateResult first = flyway.migrate();
            List<HistoryRow> firstHistory = successfulHistory();

            MigrateResult second = flyway.migrate();
            List<HistoryRow> secondHistory = successfulHistory();

            assertThat(legacy.migrationsExecuted).isEqualTo(13);
            assertThat(first.migrationsExecuted).isEqualTo(1);
            assertThat(second.migrationsExecuted).isZero();
            assertThat(secondHistory).isEqualTo(firstHistory);
            assertThat(firstHistory.subList(0, legacyHistory.size()))
                    .isEqualTo(legacyHistory);
            assertThat(secondHistory).extracting(HistoryRow::version)
                    .containsExactly(
                            "1", "2", "3", "4", "5",
                            "6", "7", "8", "9", "10", "11", "12", "13",
                            "14"
                    );
            assertThat(secondHistory).allSatisfy(history -> {
                assertThat(history.checksum()).isNotNull();
                assertThat(history.success()).isTrue();
            });
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private List<HistoryRow> successfulHistory() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT installed_rank, version, description, type, script, checksum, success
                     FROM flyway_schema_history
                     WHERE success = TRUE
                     ORDER BY installed_rank
                     """)) {
            List<HistoryRow> history = new ArrayList<>();
            while (resultSet.next()) {
                history.add(new HistoryRow(
                        resultSet.getInt("installed_rank"),
                        resultSet.getString("version"),
                        resultSet.getString("description"),
                        resultSet.getString("type"),
                        resultSet.getString("script"),
                        resultSet.getObject("checksum", Integer.class),
                        resultSet.getBoolean("success")
                ));
            }
            return history;
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
