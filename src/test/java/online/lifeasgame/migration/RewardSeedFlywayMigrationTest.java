package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V13 first-step Reward Seed migration")
class RewardSeedFlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_reward_seed_negative")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Test
    @DisplayName("stable Item이 없으면 V13이 실패하고 불완전 Profile을 남기지 않는다")
    void failsWithoutStableItem() throws Exception {
        assertThat(flyway(MigrationVersion.fromVersion("12")).migrate().migrationsExecuted)
                .isEqualTo(12);
        deleteFirstStepFragment();

        assertThatThrownBy(() -> flyway(null).migrate())
                .isInstanceOf(FlywayException.class);

        assertThat(firstStepDefinitionCount()).isZero();
        assertThat(firstStepProfileCount()).isZero();
        assertThat(firstStepProfileLineCount()).isZero();
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

    private void deleteFirstStepFragment() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    DELETE FROM items
                    WHERE code = 'IT_FIRST_STEP_FRAGMENT'
                    """);
        }
    }

    private int firstStepDefinitionCount() throws Exception {
        return count("""
                SELECT COUNT(*)
                FROM reward_definitions
                WHERE code IN (
                    'RD_EXP_20',
                    'RD_ITEM_FIRST_STEP_FRAGMENT_1'
                )
                """);
    }

    private int firstStepProfileCount() throws Exception {
        return count("""
                SELECT COUNT(*)
                FROM reward_profiles
                WHERE code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
                """);
    }

    private int firstStepProfileLineCount() throws Exception {
        return count("""
                SELECT COUNT(*)
                FROM reward_profile_lines line
                JOIN reward_profiles profile
                  ON profile.id = line.reward_profile_id
                WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
                """);
    }

    private int count(String sql) throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
