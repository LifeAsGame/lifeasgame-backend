package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V18 Reward ITEM code snapshot migration")
class RewardItemCodeSnapshotFlywayTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_reward_item_snapshot")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @Test
    @DisplayName("V17 ITEM rows를 Catalog code로 backfill하고 EXP/ITEM payload CHECK를 적용한다")
    void backfillsExistingItemRowsAndAddsChecks() {
        Flyway throughV17 = flyway(MigrationVersion.fromVersion("17"));
        throughV17.migrate();
        JdbcTemplate jdbc = jdbc();
        insertLegacySettlement(jdbc);

        Flyway flyway = flyway(null);
        flyway.migrate();

        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("18");
        assertThat(jdbc.queryForObject("""
                SELECT item_code
                FROM reward_definitions
                WHERE code = 'RD_ITEM_FIRST_STEP_FRAGMENT_1'
                """, String.class)).isEqualTo("IT_FIRST_STEP_FRAGMENT");
        assertThat(jdbc.queryForObject("""
                SELECT item_code
                FROM reward_settlement_lines
                WHERE reward_type = 'ITEM'
                """, String.class)).isEqualTo("IT_FIRST_STEP_FRAGMENT");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM reward_definitions
                WHERE reward_type = 'EXP' AND item_code IS NOT NULL
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM reward_settlement_lines
                WHERE reward_type = 'EXP' AND item_code IS NOT NULL
                """, Integer.class)).isZero();

        Set<String> checks = Set.copyOf(jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND constraint_type = 'CHECK'
                  AND table_name IN (
                      'reward_definitions', 'reward_settlement_lines'
                  )
                """, String.class));
        assertThat(checks).contains(
                "ck_reward_definition_payload",
                "ck_reward_settlement_line_payload"
        );

        assertThatThrownBy(() -> jdbc.update("""
                UPDATE reward_definitions
                SET item_code = NULL
                WHERE code = 'RD_ITEM_FIRST_STEP_FRAGMENT_1'
                """)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE reward_settlement_lines
                SET item_code = ' '
                WHERE reward_type = 'ITEM'
                """)).isInstanceOf(RuntimeException.class);
    }

    private void insertLegacySettlement(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO reward_settlements (
                    player_id, source_type, source_id,
                    reward_profile_id, reward_profile_code, status,
                    created_at, updated_at
                )
                SELECT
                    226001, 'QUEST_COMPLETION', 226101,
                    profile.id, profile.code, 'PENDING',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM reward_profiles profile
                WHERE profile.code = 'RP_EXP_AND_ITEM_FIRST_STEP_20'
                """);
        insertLegacyLine(jdbc, "RD_EXP_20", 0);
        insertLegacyLine(jdbc, "RD_ITEM_FIRST_STEP_FRAGMENT_1", 1);
    }

    private void insertLegacyLine(
            JdbcTemplate jdbc,
            String definitionCode,
            int sortOrder
    ) {
        jdbc.update("""
                INSERT INTO reward_settlement_lines (
                    reward_settlement_id,
                    reward_definition_id,
                    reward_definition_code,
                    reward_type,
                    amount,
                    item_id,
                    sort_order,
                    status,
                    failure_code,
                    created_at,
                    updated_at
                )
                SELECT
                    settlement.id,
                    definition.id,
                    definition.code,
                    definition.reward_type,
                    definition.amount,
                    definition.item_id,
                    ?,
                    'PENDING',
                    NULL,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                FROM reward_settlements settlement
                JOIN reward_definitions definition
                  ON definition.code = ?
                WHERE settlement.source_id = 226101
                """, sortOrder, definitionCode);
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .baselineOnMigrate(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
    }
}
