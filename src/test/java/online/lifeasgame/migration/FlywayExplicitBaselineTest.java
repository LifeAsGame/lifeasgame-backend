package online.lifeasgame.migration;

import online.lifeasgame.LifeasgameApplication;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("Flyway explicit baseline")
class FlywayExplicitBaselineTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_explicit_baseline")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Nested
    @DisplayName("V1 상당 schema가 있지만 Flyway history가 없을 때")
    class RejectAutomaticBaseline {

        @Test
        @DisplayName("baselineOnMigrate false는 migrate를 거부하고 marker를 만들지 않는다")
        void rejectsMigrationWithoutHistory() throws Exception {
            String jdbcUrl = createV1EquivalentDatabase();
            Flyway flyway = migrationFlyway(jdbcUrl);

            assertThatThrownBy(flyway::migrate)
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("non-empty schema")
                    .hasMessageContaining("baseline");

            assertThat(tableCount(jdbcUrl, "flyway_schema_history")).isZero();
            assertThat(tableCount(jdbcUrl, "reward_profiles")).isZero();
            assertThat(userCount(jdbcUrl)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("V1 상당 schema에 version 1 baseline을 명시적으로 기록할 때")
    class ApplyExplicitBaseline {

        @Test
        @DisplayName("V1을 재실행하지 않고 V2부터 V21까지 적용한 뒤 JPA validate를 통과한다")
        void migratesFromVersionTwoAndValidatesJpa() throws Exception {
            String jdbcUrl = createV1EquivalentDatabase();
            Flyway flyway = migrationFlyway(jdbcUrl);

            flyway.baseline();
            MigrateResult migrateResult = flyway.migrate();
            List<HistoryRow> history = successfulHistory(jdbcUrl);

            assertThat(migrateResult.migrationsExecuted).isEqualTo(20);
            assertThat(history).extracting(HistoryRow::version)
                    .containsExactly(
                            "1", "2", "3", "4", "5",
                            "6", "7", "8", "9", "10", "11", "12", "13",
                            "14", "15", "16", "17", "18", "19", "20",
                            "21"
                    );
            assertThat(history.getFirst().type()).isEqualTo("BASELINE");
            assertThat(history.getFirst().script())
                    .isEqualTo("baseline current production schema");
            assertThat(history.getFirst().checksum()).isNull();
            assertThat(history.subList(1, history.size()))
                    .extracting(HistoryRow::type)
                    .containsOnly("SQL");
            assertThat(history.subList(1, history.size()))
                    .extracting(HistoryRow::script)
                    .containsExactly(
                            "V2__reward_definition_foundation.sql",
                            "V3__reward_settlement_foundation.sql",
                            "V4__player_growth_change.sql",
                            "V5__reward_none_profile.sql",
                            "V6__quest_state_contract.sql",
                            "V7__quest_signal_receipt.sql",
                            "V8__transactional_outbox.sql",
                            "V9__quick_lifelog_record.sql",
                            "V10__quest_definition_reward_profile_contract.sql",
                            "V11__quest_semantic_progress_contract.sql",
                            "V12__item_stable_code_and_first_step_seed.sql",
                            "V13__first_step_reward_profile_seed.sql",
                            "V14__final_quest_legacy_category_nullable.sql",
                            "V15__canonical_lifelog_record_metadata.sql",
                            "V16__quest_acceptance_fact_context.sql",
                            "V17__inventory_reward_delivery_foundation.sql",
                            "V18__reward_item_code_snapshot.sql",
                            "V19__role_person_persistence_foundation.sql",
                            "V20__quest_route_mvp.sql",
                            "V21__role_event_lifelog_linkage.sql"
                    );
            assertThat(history.subList(1, history.size()))
                    .allSatisfy(row -> assertThat(row.checksum()).isNotNull());
            assertThat(history).allSatisfy(row -> assertThat(row.success()).isTrue());
            assertThat(userCount(jdbcUrl)).isEqualTo(1);
            assertThat(noRewardProfile(jdbcUrl))
                    .isEqualTo(new NoRewardProfile("ACTIVE", 0));
            assertThat(uniqueIndexColumns(
                    jdbcUrl,
                    "reward_settlements",
                    "uq_reward_settlement_source"
            )).containsExactly("player_id", "source_type", "source_id");

            try (ConfigurableApplicationContext context = startValidationContext(jdbcUrl)) {
                assertThat(context.isActive()).isTrue();
                assertThat(context.getEnvironment().getProperty(
                        "spring.jpa.hibernate.ddl-auto"
                )).isEqualTo("validate");
                assertThat(context.getBean(Flyway.class).info().current().getVersion().getVersion())
                        .isEqualTo("21");
            }
        }
    }

    private String createV1EquivalentDatabase() throws Exception {
        String jdbcUrl = MYSQL.getJdbcUrl();
        Flyway.configure()
                .dataSource(jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword())
                .cleanDisabled(false)
                .load()
                .clean();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        );
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V1__baseline_current_schema.sql"
        )).execute(dataSource);
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO users (
                        id, email, password_hash, nickname, status, created_at, updated_at
                    ) VALUES (
                        191, 'baseline@example.com', 'hash', 'baseline', 'ACTIVE',
                        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                    )
                    """);
        }
        return jdbcUrl;
    }

    private Flyway migrationFlyway(String jdbcUrl) {
        return Flyway.configure()
                .dataSource(jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .baselineVersion("1")
                .baselineDescription("baseline current production schema")
                .load();
    }

    private ConfigurableApplicationContext startValidationContext(String jdbcUrl) {
        return new SpringApplicationBuilder(LifeasgameApplication.class)
                .web(WebApplicationType.SERVLET)
                .registerShutdownHook(false)
                .run(
                        "--spring.profiles.active=test,migration-test",
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=" + MYSQL.getUsername(),
                        "--spring.datasource.password=" + MYSQL.getPassword(),
                        "--spring.datasource.driver-class-name=" + MYSQL.getDriverClassName(),
                        "--server.port=0",
                        "--spring.main.banner-mode=off"
                );
    }

    private int tableCount(String jdbcUrl, String tableName) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int userCount(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT COUNT(*)
                     FROM users
                     WHERE id = 191
                       AND email = 'baseline@example.com'
                     """)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private List<HistoryRow> successfulHistory(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT version, type, script, checksum, success
                     FROM flyway_schema_history
                     WHERE success = TRUE
                     ORDER BY installed_rank
                     """)) {
            List<HistoryRow> history = new ArrayList<>();
            while (resultSet.next()) {
                history.add(new HistoryRow(
                        resultSet.getString("version"),
                        resultSet.getString("type"),
                        resultSet.getString("script"),
                        resultSet.getObject("checksum", Integer.class),
                        resultSet.getBoolean("success")
                ));
            }
            return history;
        }
    }

    private NoRewardProfile noRewardProfile(String jdbcUrl) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT profile.status, COUNT(line.id) AS line_count
                     FROM reward_profiles profile
                     LEFT JOIN reward_profile_lines line ON line.reward_profile_id = profile.id
                     WHERE profile.code = 'RP_NONE'
                     GROUP BY profile.id, profile.status
                     """)) {
            assertThat(resultSet.next()).isTrue();
            return new NoRewardProfile(
                    resultSet.getString("status"),
                    resultSet.getInt("line_count")
            );
        }
    }

    private List<String> uniqueIndexColumns(
            String jdbcUrl,
            String tableName,
            String indexName
    ) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                jdbcUrl, MYSQL.getUsername(), MYSQL.getPassword()
        ); var statement = connection.prepareStatement("""
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

    private record HistoryRow(
            String version,
            String type,
            String script,
            Integer checksum,
            boolean success
    ) {
    }

    private record NoRewardProfile(String status, int lineCount) {
    }
}
