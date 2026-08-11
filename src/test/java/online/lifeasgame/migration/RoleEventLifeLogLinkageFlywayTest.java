package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V21 RoleEvent와 LifeLog linkage migration")
class RoleEventLifeLogLinkageFlywayTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_role_event_migration")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    private Flyway flyway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateCleanDatabase() {
        flyway = flyway(null);
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
    }

    @Nested
    @DisplayName("빈 MySQL에 V21을 적용하면")
    class CreateFoundation {

        @Test
        @DisplayName("RoleEvent 두 table과 canonical role_event_id를 추가한다")
        void createsTablesAndColumn() {
            assertThat(flyway.info().current().getVersion().getVersion())
                    .isEqualTo("21");
            assertThat(tableNames()).containsExactlyInAnyOrder(
                    "role_event_participants",
                    "role_events"
            );
            assertThat(columnNames("life_log_records"))
                    .contains("primary_role_id", "role_event_id")
                    .doesNotContain("role_party_id", "role_chat_id");
            assertThat(foreignKeys()).contains(
                    "fk_role_event_role_owner",
                    "fk_role_event_participant_event",
                    "fk_life_log_record_role_owner",
                    "fk_life_log_record_role_event_context"
            );
        }

        @Test
        @DisplayName("같은 participant type과 ID만 중복을 막고 두 type 의미는 분리한다")
        void enforcesParticipantIdentity() {
            Long roleId = insertRole(1L, "Developer");
            Long eventId = insertRoleEvent(1L, roleId);

            insertParticipant(eventId, "PERSON", 7L);
            insertParticipant(eventId, "SERVICE_USER", 7L);

            assertThat(participantCount(eventId)).isEqualTo(2);
            assertThatThrownBy(() -> insertParticipant(
                    eventId,
                    "PERSON",
                    7L
            )).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("canonical LifeLog에 RoleEvent를 연결하면")
    class LinkLifeLog {

        @Test
        @DisplayName("같은 Player와 Role의 Event linkage만 허용한다")
        void enforcesRoleEventPlayerConsistency() {
            Long roleId = insertRole(1L, "Developer");
            Long otherRoleId = insertRole(1L, "Parent");
            Long eventId = insertRoleEvent(1L, roleId);

            insertLifeLog(1L, 101L, roleId, eventId);
            insertLifeLog(1L, 102L, roleId, null);

            assertThatThrownBy(() -> insertLifeLog(
                    1L,
                    103L,
                    otherRoleId,
                    eventId
            )).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insertLifeLog(
                    2L,
                    104L,
                    roleId,
                    null
            )).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("V20의 기존 LifeLog를 V21로 올리면")
    class UpgradeExistingRows {

        @Test
        @DisplayName("Role context가 없던 기존 row를 그대로 유지한다")
        void keepsExistingLifeLogRowsValid() {
            flyway.clean();
            flyway(MigrationVersion.fromVersion("20")).migrate();
            insertLifeLogBeforeV21(1L, 201L);

            flyway.migrate();

            assertThat(jdbc.queryForMap("""
                    SELECT primary_role_id, role_event_id
                    FROM life_log_records
                    WHERE source_type = 'COLLECTION' AND source_id = 201
                    """))
                    .containsEntry("primary_role_id", null)
                    .containsEntry("role_event_id", null);
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
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private Set<String> tableNames() {
        return Set.copyOf(jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('role_events', 'role_event_participants')
                """, String.class));
    }

    private Set<String> columnNames(String table) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table));
    }

    private Set<String> foreignKeys() {
        return Set.copyOf(jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name IN (
                    'role_events',
                    'role_event_participants',
                    'life_log_records'
                  )
                """, String.class));
    }

    private Long insertRole(Long playerId, String name) {
        jdbc.update("""
                INSERT INTO roles (
                    player_id, role_type, name, description, status,
                    created_at, updated_at, version
                ) VALUES (?, 'SELF', ?, NULL, 'ACTIVE',
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), 0)
                """, playerId, name);
        return jdbc.queryForObject(
                "SELECT id FROM roles WHERE player_id = ? AND name = ?",
                Long.class,
                playerId,
                name
        );
    }

    private Long insertRoleEvent(Long playerId, Long roleId) {
        jdbc.update("""
                INSERT INTO role_events (
                    player_id, role_id, title, description,
                    starts_at, ends_at, status, completed_at,
                    version, created_at, updated_at
                ) VALUES (?, ?, '회고', NULL, NULL, NULL, 'PLANNED', NULL,
                    0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, roleId);
        return jdbc.queryForObject(
                "SELECT id FROM role_events WHERE player_id = ? AND role_id = ?",
                Long.class,
                playerId,
                roleId
        );
    }

    private void insertParticipant(
            Long eventId,
            String type,
            Long participantId
    ) {
        jdbc.update("""
                INSERT INTO role_event_participants (
                    role_event_id, participant_type, participant_id,
                    created_at, updated_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, eventId, type, participantId);
    }

    private int participantCount(Long eventId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_event_participants WHERE role_event_id = ?",
                Integer.class,
                eventId
        );
    }

    private void insertLifeLog(
            Long playerId,
            Long sourceId,
            Long roleId,
            Long eventId
    ) {
        jdbc.update("""
                INSERT INTO life_log_records (
                    player_id, source_type, source_id,
                    source_definition_version, subtype, entry_mode,
                    reflection_scope, period_key,
                    primary_role_id, role_event_id, occurred_at,
                    created_at, updated_at
                ) VALUES (?, 'COLLECTION', ?, 1, NULL, 'FULL',
                    NULL, NULL, ?, ?, CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, sourceId, roleId, eventId);
    }

    private void insertLifeLogBeforeV21(Long playerId, Long sourceId) {
        jdbc.update("""
                INSERT INTO life_log_records (
                    player_id, source_type, source_id,
                    source_definition_version, subtype, entry_mode,
                    reflection_scope, period_key, primary_role_id,
                    occurred_at, created_at, updated_at
                ) VALUES (?, 'COLLECTION', ?, 1, NULL, 'FULL',
                    NULL, NULL, NULL, CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, sourceId);
    }
}
