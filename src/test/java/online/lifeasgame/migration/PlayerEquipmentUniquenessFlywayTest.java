package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V23 Player equipment item uniqueness migration")
class PlayerEquipmentUniquenessFlywayTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_equipment_uniqueness")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateThroughV22() {
        Flyway throughV22 = flyway(MigrationVersion.fromVersion("22"));
        throughV22.clean();
        throughV22.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
    }

    @Nested
    @DisplayName("중복 장착 데이터가 없으면")
    class CompatibleExistingData {

        @Test
        @DisplayName("player/item unique를 추가하고 nullable empty slot은 유지한다")
        void addsPlayerItemUniquenessWithNullableSlots() {
            var result = flyway(null).migrate();

            assertThat(result.migrationsExecuted).isEqualTo(1);
            assertThat(indexColumns()).containsExactly(
                    "player_id",
                    "item_inst_id"
            );
            assertThatCode(() -> {
                insertEquipment(262L, 1L, null);
                insertEquipment(262L, 2L, null);
                insertEquipment(262L, 3L, 26201L);
                insertEquipment(263L, 1L, 26201L);
            }).doesNotThrowAnyException();
            assertThatThrownBy(() -> insertEquipment(
                    262L,
                    4L,
                    26201L
            )).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("기존 Player에 non-null duplicate item이 있으면")
    class ConflictingExistingData {

        @Test
        @DisplayName("cleanup 없이 migration을 실패시키고 원본 두 행을 보존한다")
        void failsClosedWithoutDeletingUnknownData() {
            insertEquipment(262L, 1L, 26201L);
            insertEquipment(262L, 2L, 26201L);

            assertThatThrownBy(() -> flyway(null).migrate())
                    .isInstanceOf(FlywayException.class);

            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM player_equipment
                    WHERE player_id = 262
                      AND item_inst_id = 26201
                    """, Integer.class)).isEqualTo(2);
            assertThat(indexColumns()).isEmpty();
            assertThat(jdbc.queryForObject("""
                    SELECT success
                    FROM flyway_schema_history
                    WHERE version = '23'
                    """, Boolean.class)).isFalse();
        }
    }

    private Flyway flyway(MigrationVersion target) {
        var configuration = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private List<String> indexColumns() {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'player_equipment'
                  AND index_name = 'uq_player_equipment_item'
                ORDER BY seq_in_index
                """, String.class);
    }

    private void insertEquipment(
            Long playerId,
            Long slotId,
            Long itemInstanceId
    ) {
        jdbc.update("""
                INSERT INTO player_equipment (
                    created_at,
                    equipped_at,
                    item_inst_id,
                    player_id,
                    slot_id,
                    updated_at
                ) VALUES (
                    CURRENT_TIMESTAMP(6),
                    CASE WHEN ? IS NULL THEN NULL ELSE CURRENT_TIMESTAMP(6) END,
                    ?, ?, ?, CURRENT_TIMESTAMP(6)
                )
                """,
                itemInstanceId,
                itemInstanceId,
                playerId,
                slotId
        );
    }
}
