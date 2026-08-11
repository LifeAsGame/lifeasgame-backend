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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V20 QuestRoute MVP migration")
class QuestRouteFlywayMigrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_route_migration")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    private Flyway flyway;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateCleanDatabase() {
        flyway = Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("20"))
                .baselineOnMigrate(false)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
    }

    @Nested
    @DisplayName("빈 MySQL에 V20을 적용하면")
    class CreateQuestRouteFoundation {

        @Test
        @DisplayName("definition/runtime 4개 table과 필요한 constraint만 만든다")
        void createsSeparatedDefinitionAndRuntimeTables() {
            assertThat(flyway.info().current().getVersion().getVersion())
                    .isEqualTo("20");
            assertThat(tableNames()).containsExactly(
                    "player_quest_routes",
                    "quest_route_step_quests",
                    "quest_route_steps",
                    "quest_routes"
            );
            assertThat(uniqueIndexes("player_quest_routes"))
                    .containsExactly("PRIMARY", "uq_player_quest_route");
            assertThat(indexColumns(
                    "player_quest_routes",
                    "uq_player_quest_route"
            )).containsExactly("player_id", "route_id");
            assertThat(foreignKeys()).containsExactlyInAnyOrder(
                    "fk_player_quest_route_current_step",
                    "fk_player_quest_route_route",
                    "fk_quest_route_step_quest_quest",
                    "fk_quest_route_step_quest_step",
                    "fk_quest_route_step_route"
            );
            assertThat(columnNames("quest_routes"))
                    .contains("primary_role_template_code")
                    .doesNotContain("role_id", "reward_id", "achievement_id");
            assertThat(columnNames("player_quest_routes"))
                    .doesNotContain("role_id", "reward_id", "achievement_id");
        }

        @Test
        @DisplayName("Player별 Route 중복만 막고 다른 Route 선택 가능성은 유지한다")
        void scopesUniquenessToPlayerAndRoute() {
            Long routeId = routeId("ROUTE_RECORD_START");
            Long firstStepId = stepId(routeId, 1);
            insertPlayerRoute(2501L, routeId, firstStepId);

            assertThatThrownBy(() -> insertPlayerRoute(
                    2501L,
                    routeId,
                    firstStepId
            )).isInstanceOf(DataIntegrityViolationException.class);

            Long secondRouteId = insertSecondRoute();
            Long secondStepId = insertSecondRouteStep(secondRouteId);
            insertPlayerRoute(2501L, secondRouteId, secondStepId);

            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM player_quest_routes WHERE player_id = 2501",
                    Integer.class
            )).isEqualTo(2);
            assertThatThrownBy(() -> insertPlayerRoute(
                    2502L,
                    secondRouteId,
                    firstStepId
            )).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("대표 Route content를 seed하면")
    class SeedRepresentativeRoute {

        @Test
        @DisplayName("ROUTE_RECORD_START 하나와 순서가 고정된 세 Step만 활성화한다")
        void seedsOnlyRecordStartRoute() {
            assertThat(jdbc.queryForList(
                    "SELECT code FROM quest_routes ORDER BY id",
                    String.class
            )).containsExactly("ROUTE_RECORD_START");
            assertThat(seedSteps()).containsExactly(
                    new SeedStep(
                            "RS_RECORD_01_LEAVE_TRACE",
                            1,
                            "첫 흔적 남기기",
                            "QUEST_COMPLETION_SET",
                            1,
                            true,
                            true,
                            false,
                            "Q_RECORD_FIRST_TRACE",
                            "REQUIRED"
                    ),
                    new SeedStep(
                            "RS_RECORD_02_CONNECT_TRACES",
                            2,
                            "흔적 연결하기",
                            "QUEST_COMPLETION_SET",
                            1,
                            true,
                            true,
                            false,
                            "Q_RECORD_THREE_TRACES",
                            "REQUIRED"
                    ),
                    new SeedStep(
                            "RS_RECORD_03_LOOK_BACK",
                            3,
                            "돌아보기",
                            "QUEST_COMPLETION_SET",
                            1,
                            true,
                            true,
                            false,
                            "Q_RECORD_WEEKLY_LOOKBACK",
                            "REQUIRED"
                    )
            );
        }

        @Test
        @DisplayName("동일한 stable Quest code가 있으면 기존 정의를 덮어쓰지 않는다")
        void preservesExistingQuestDefinitionOnStableCodeConflict() {
            flyway.clean();
            Flyway.configure()
                    .dataSource(
                            MYSQL.getJdbcUrl(),
                            MYSQL.getUsername(),
                            MYSQL.getPassword()
                    )
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("19"))
                    .baselineOnMigrate(false)
                    .cleanDisabled(false)
                    .load()
                    .migrate();
            jdbc.update("""
                    INSERT INTO quests (
                        reward_exp,
                        definition_version,
                        target_value,
                        created_at,
                        due_at,
                        updated_at,
                        code,
                        title_id,
                        reward_stats,
                        reward_profile_code,
                        category,
                        semantic_category,
                        description_md,
                        repeat_rule,
                        completion_policy,
                        role_template_code,
                        target_type,
                        progress_source
                    ) VALUES (
                        0, 7, 1, CURRENT_TIMESTAMP(6), NULL,
                        CURRENT_TIMESTAMP(6), 'Q_RECORD_FIRST_TRACE',
                        '기존 정의', JSON_OBJECT(), 'RP_NONE', NULL,
                        'RECORD', '기존 설명', 'ONCE', 'AUTO', NULL,
                        'COUNT', 'RECORD_CREATED'
                    )
                    """);

            flyway.migrate();

            assertThat(jdbc.queryForMap("""
                    SELECT definition_version, title_id
                    FROM quests
                    WHERE code = 'Q_RECORD_FIRST_TRACE'
                    """))
                    .containsEntry("definition_version", 7)
                    .containsEntry("title_id", "기존 정의");
            assertThat(routeId("ROUTE_RECORD_START")).isPositive();
        }
    }

    private List<String> tableNames() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                    'quest_routes',
                    'quest_route_steps',
                    'quest_route_step_quests',
                    'player_quest_routes'
                  )
                ORDER BY table_name
                """, String.class);
    }

    private Set<String> columnNames(String tableName) {
        return Set.copyOf(jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, String.class, tableName));
    }

    private List<String> uniqueIndexes(String tableName) {
        return jdbc.queryForList("""
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND non_unique = 0
                ORDER BY index_name
                """, String.class, tableName);
    }

    private List<String> indexColumns(String tableName, String indexName) {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND index_name = ?
                ORDER BY seq_in_index
                """, String.class, tableName, indexName);
    }

    private Set<String> foreignKeys() {
        return Set.copyOf(jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.referential_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name IN (
                    'quest_route_steps',
                    'quest_route_step_quests',
                    'player_quest_routes'
                  )
                """, String.class));
    }

    private List<SeedStep> seedSteps() {
        return jdbc.query("""
                SELECT
                    step.step_code,
                    step.step_order,
                    step.title,
                    step.criterion_type,
                    step.required_evidence_count,
                    step.user_advance_required,
                    step.retroactive_evidence_allowed,
                    step.skip_allowed,
                    quest.code AS quest_code,
                    link.requirement_type
                FROM quest_route_steps step
                JOIN quest_routes route ON route.id = step.route_id
                JOIN quest_route_step_quests link ON link.step_id = step.id
                JOIN quests quest ON quest.id = link.quest_id
                WHERE route.code = 'ROUTE_RECORD_START'
                ORDER BY step.step_order
                """, (resultSet, rowNumber) -> new SeedStep(
                resultSet.getString("step_code"),
                resultSet.getInt("step_order"),
                resultSet.getString("title"),
                resultSet.getString("criterion_type"),
                resultSet.getInt("required_evidence_count"),
                resultSet.getBoolean("user_advance_required"),
                resultSet.getBoolean("retroactive_evidence_allowed"),
                resultSet.getBoolean("skip_allowed"),
                resultSet.getString("quest_code"),
                resultSet.getString("requirement_type")
        ));
    }

    private Long routeId(String code) {
        return jdbc.queryForObject(
                "SELECT id FROM quest_routes WHERE code = ?",
                Long.class,
                code
        );
    }

    private Long stepId(Long routeId, int stepOrder) {
        return jdbc.queryForObject(
                "SELECT id FROM quest_route_steps WHERE route_id = ? AND step_order = ?",
                Long.class,
                routeId,
                stepOrder
        );
    }

    private Long insertSecondRoute() {
        jdbc.update("""
                INSERT INTO quest_routes (
                    code, definition_version, title, description,
                    primary_role_template_code, created_at, updated_at
                ) VALUES (
                    'ROUTE_TEST_SECOND', 1, 'Second', NULL, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        return routeId("ROUTE_TEST_SECOND");
    }

    private Long insertSecondRouteStep(Long routeId) {
        jdbc.update("""
                INSERT INTO quest_route_steps (
                    route_id, step_code, step_order, title, description,
                    criterion_type, required_evidence_count,
                    user_advance_required, retroactive_evidence_allowed,
                    skip_allowed
                ) VALUES (
                    ?, 'ROUTE_TEST_SECOND_STEP', 1, 'Second step', NULL,
                    'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0'
                )
                """, routeId);
        return stepId(routeId, 1);
    }

    private void insertPlayerRoute(
            Long playerId,
            Long routeId,
            Long currentStepId
    ) {
        jdbc.update("""
                INSERT INTO player_quest_routes (
                    player_id, route_id, current_step_id, status,
                    selected_at, completed_at, version,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, ?, 'IN_PROGRESS', CURRENT_TIMESTAMP(6), NULL, 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, playerId, routeId, currentStepId);
    }

    private record SeedStep(
            String stepCode,
            int stepOrder,
            String title,
            String criterionType,
            int requiredEvidenceCount,
            boolean userAdvanceRequired,
            boolean retroactiveEvidenceAllowed,
            boolean skipAllowed,
            String questCode,
            String requirementType
    ) {
    }
}
