package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.quest.domain.PlayerQuestRouteStatus;
import online.lifeasgame.quest.domain.QuestRouteStepState;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.PlayerQuestRouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("QuestRoute Application 흐름")
class QuestRouteApplicationIntegrationTest {

    private static final Long PLAYER_ID = 250001L;
    private static final Long OTHER_PLAYER_ID = 250002L;
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-09T01:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_route")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private QuestRouteSelectService selectService;

    @Autowired
    private QuestRouteAdvanceService advanceService;

    @Autowired
    private QuestRouteQueryService queryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlayerQuestRouteRepository playerQuestRouteRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @BeforeEach
    void setUp() {
        when(currentPlayerAccessor.currentPlayerIdOrThrow())
                .thenReturn(PLAYER_ID);
        jdbcTemplate.update(
                "DELETE FROM player_quest_routes WHERE player_id IN (?, ?)",
                PLAYER_ID,
                OTHER_PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id IN (?, ?)",
                PLAYER_ID,
                OTHER_PLAYER_ID
        );
        deleteTestRoute();
        jdbcTemplate.update("""
                DELETE link
                FROM quest_route_step_quests link
                JOIN quest_route_steps step ON step.id = link.step_id
                JOIN quest_routes route ON route.id = step.route_id
                JOIN quests quest ON quest.id = link.quest_id
                WHERE route.code = 'ROUTE_RECORD_START'
                  AND step.step_code = 'RS_RECORD_01_LEAVE_TRACE'
                  AND quest.code = 'Q_RECORD_THREE_TRACES'
                  AND link.requirement_type = 'OPTIONAL'
                """);
    }

    @Nested
    @DisplayName("Route를 선택할 때")
    class SelectRoute {

        @Test
        @DisplayName("처음 선택하면 첫 Step이 current이고 Quest는 자동 수락하지 않는다")
        void selectsFirstStepWithoutAcceptingQuest() {
            QuestRouteResult.Route selected = selectService.select(routeId());

            assertThat(selected.playerProgress().status())
                    .isEqualTo(PlayerQuestRouteStatus.IN_PROGRESS.name());
            assertThat(selected.playerProgress().currentStepId())
                    .isEqualTo(stepId("RS_RECORD_01_LEAVE_TRACE"));
            assertThat(selected.steps()).extracting(QuestRouteResult.Step::state)
                    .containsExactly(
                            QuestRouteStepState.CURRENT.name(),
                            QuestRouteStepState.LOCKED.name(),
                            QuestRouteStepState.LOCKED.name()
                    );
            assertThat(acceptanceCount(PLAYER_ID)).isZero();
        }

        @Test
        @DisplayName("같은 Route를 순차 재선택하면 기존 runtime을 반환한다")
        void returnsExistingRuntimeOnSequentialReplay() {
            QuestRouteResult.Route first = selectService.select(routeId());
            QuestRouteResult.Route replay = selectService.select(routeId());

            assertThat(replay.playerProgress().id())
                    .isEqualTo(first.playerProgress().id());
            assertThat(playerRouteCount(PLAYER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 Route를 동시에 선택해도 runtime은 하나만 생성된다")
        void createsOneRuntimeOnConcurrentSelection() throws Exception {
            List<String> outcomes = runConcurrently(
                    () -> selectService.select(routeId()).code()
            );

            assertThat(outcomes).containsOnly("ROUTE_RECORD_START");
            assertThat(playerRouteCount(PLAYER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("서로 다른 Player가 같은 Route를 동시에 선택하면 각각 runtime을 생성한다")
        void createsIndependentRuntimesAcrossPlayers() throws Exception {
            Long routeId = routeId();
            when(currentPlayerAccessor.currentPlayerIdOrThrow())
                    .thenReturn(PLAYER_ID, OTHER_PLAYER_ID);

            List<String> outcomes = runConcurrently(
                    () -> selectService.select(routeId).code()
            );

            assertThat(outcomes).containsOnly("ROUTE_RECORD_START");
            assertThat(playerRouteCount(PLAYER_ID)).isEqualTo(1);
            assertThat(playerRouteCount(OTHER_PLAYER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("다른 Route를 선택해도 기존 Route를 취소하지 않는다")
        void keepsMultipleSelectedRoutes() {
            Long anotherRouteId = insertAnotherRoute();

            selectService.select(routeId());
            selectService.select(anotherRouteId);

            assertThat(playerRouteCount(PLAYER_ID)).isEqualTo(2);
            assertThat(playerRouteStatuses(PLAYER_ID))
                    .containsOnly(PlayerQuestRouteStatus.IN_PROGRESS.name());
        }
    }

    @Nested
    @DisplayName("선택 runtime을 DB에 생성할 때")
    class PersistSelectedRoute {

        @Test
        @DisplayName("존재하지 않는 Route 또는 Step 참조의 무결성 실패를 전파한다")
        void propagatesForeignKeyViolation() {
            TransactionTemplate transaction =
                    new TransactionTemplate(transactionManager);
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");

            assertThatThrownBy(() -> transaction.executeWithoutResult(
                    status -> playerQuestRouteRepository.insertIfAbsent(
                            PLAYER_ID,
                            Long.MAX_VALUE,
                            firstStepId,
                            COMPLETED_AT
                    )
            )).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> transaction.executeWithoutResult(
                    status -> playerQuestRouteRepository.insertIfAbsent(
                            PLAYER_ID,
                            routeId,
                            Long.MAX_VALUE,
                            COMPLETED_AT
                    )
            )).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("현재 Step의 Quest completion criteria를 평가할 때")
    class EvaluateCriteria {

        @Test
        @DisplayName("required Quest가 미완료이면 current 상태를 유지한다")
        void keepsCurrentWhenRequiredQuestIsIncomplete() {
            selectService.select(routeId());

            QuestRouteResult.Step current = queryService.myRoute(routeId())
                    .steps().getFirst();

            assertThat(current.criteriaSatisfied()).isFalse();
            assertThat(current.state())
                    .isEqualTo(QuestRouteStepState.CURRENT.name());
        }

        @Test
        @DisplayName("Route 선택 전에 완료한 required Quest도 인정한다")
        void countsRetroactiveQuestCompletion() {
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );

            selectService.select(routeId());
            QuestRouteResult.Step current = queryService.myRoute(routeId())
                    .steps().getFirst();

            assertThat(current.criteriaSatisfied()).isTrue();
            assertThat(current.state())
                    .isEqualTo(QuestRouteStepState.READY_TO_ADVANCE.name());
        }

        @Test
        @DisplayName("Optional Quest가 미완료여도 required criterion을 막지 않는다")
        void ignoresIncompleteOptionalQuest() {
            addOptionalQuestToFirstStep();
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );
            selectService.select(routeId());

            QuestRouteResult.Step current = queryService.myRoute(routeId())
                    .steps().getFirst();

            assertThat(current.criteriaSatisfied()).isTrue();
            assertThat(current.questLinks()).hasSize(2);
        }

        @Test
        @DisplayName("다른 Player의 Quest 완료는 인정하지 않는다")
        void ignoresAnotherPlayersCompletion() {
            completeQuest(
                    OTHER_PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );
            selectService.select(routeId());

            QuestRouteResult.Step current = queryService.myRoute(routeId())
                    .steps().getFirst();

            assertThat(current.criteriaSatisfied()).isFalse();
            assertThat(current.state())
                    .isEqualTo(QuestRouteStepState.CURRENT.name());
        }
    }

    @Nested
    @DisplayName("현재 Step을 명시적으로 advance할 때")
    class AdvanceCurrentStep {

        @Test
        @DisplayName("criteria가 충족되지 않으면 상태를 변경하지 않는다")
        void rejectsUnsatisfiedCriteria() {
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");
            selectService.select(routeId);

            assertRouteError(
                    () -> advanceService.advance(routeId, firstStepId),
                    QuestError.ROUTE_STEP_CRITERIA_NOT_SATISFIED
            );
            assertThat(currentStepId(PLAYER_ID, routeId))
                    .isEqualTo(firstStepId);
        }

        @Test
        @DisplayName("Quest 완료만으로 current Step은 이동하지 않는다")
        void neverAutoAdvancesOnQuestCompletion() {
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");
            selectService.select(routeId);

            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );

            assertThat(currentStepId(PLAYER_ID, routeId))
                    .isEqualTo(firstStepId);
            assertThat(queryService.myRoute(routeId).steps().getFirst().state())
                    .isEqualTo(QuestRouteStepState.READY_TO_ADVANCE.name());
        }

        @Test
        @DisplayName("criteria가 충족되면 정확히 다음 한 Step으로 이동한다")
        void advancesExactlyOneStep() {
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");
            Long secondStepId = stepId("RS_RECORD_02_CONNECT_TRACES");
            selectService.select(routeId);
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );

            QuestRouteResult.Route advanced = advanceService.advance(
                    routeId,
                    firstStepId
            );

            assertThat(advanced.playerProgress().currentStepId())
                    .isEqualTo(secondStepId);
            assertThat(currentStepId(PLAYER_ID, routeId))
                    .isEqualTo(secondStepId);
        }

        @Test
        @DisplayName("같은 expectedStepId replay는 두 번째 Step까지 넘기지 않는다")
        void rejectsStaleReplay() {
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");
            Long secondStepId = stepId("RS_RECORD_02_CONNECT_TRACES");
            selectService.select(routeId);
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );
            advanceService.advance(routeId, firstStepId);

            assertRouteError(
                    () -> advanceService.advance(routeId, firstStepId),
                    QuestError.ROUTE_STEP_NOT_CURRENT
            );
            assertThat(currentStepId(PLAYER_ID, routeId))
                    .isEqualTo(secondStepId);
        }

        @Test
        @DisplayName("동일 Step 동시 advance는 최대 한 번만 전이한다")
        void serializesConcurrentAdvance() throws Exception {
            Long routeId = routeId();
            Long firstStepId = stepId("RS_RECORD_01_LEAVE_TRACE");
            Long secondStepId = stepId("RS_RECORD_02_CONNECT_TRACES");
            selectService.select(routeId);
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );

            List<String> outcomes = runConcurrently(() -> {
                try {
                    advanceService.advance(routeId, firstStepId);
                    return "ADVANCED";
                } catch (DomainException exception) {
                    return exception.code();
                }
            });

            assertThat(outcomes).containsExactlyInAnyOrder(
                    "ADVANCED",
                    QuestError.ROUTE_STEP_NOT_CURRENT.code()
            );
            assertThat(currentStepId(PLAYER_ID, routeId))
                    .isEqualTo(secondStepId);
        }

        @Test
        @DisplayName("마지막 Step advance는 Route를 완료하고 replay를 거부한다")
        void completesFinalStepOnce() {
            Long routeId = routeId();
            Long first = stepId("RS_RECORD_01_LEAVE_TRACE");
            Long second = stepId("RS_RECORD_02_CONNECT_TRACES");
            Long third = stepId("RS_RECORD_03_LOOK_BACK");
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_THREE_TRACES",
                    COMPLETED_AT.plusSeconds(1)
            );
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_WEEKLY_LOOKBACK",
                    COMPLETED_AT.plusSeconds(2)
            );
            selectService.select(routeId);
            advanceService.advance(routeId, first);
            advanceService.advance(routeId, second);

            QuestRouteResult.Route completed = advanceService.advance(
                    routeId,
                    third
            );

            assertThat(completed.playerProgress().status())
                    .isEqualTo(PlayerQuestRouteStatus.COMPLETED.name());
            assertThat(completed.playerProgress().completedAt()).isNotNull();
            assertThat(completed.steps())
                    .extracting(QuestRouteResult.Step::state)
                    .containsOnly(QuestRouteStepState.COMPLETED.name());
            assertRouteError(
                    () -> advanceService.advance(routeId, third),
                    QuestError.ROUTE_ALREADY_COMPLETED
            );
        }
    }

    @Nested
    @DisplayName("Route 진행 상태를 조회할 때")
    class QueryRouteProgress {

        @Test
        @DisplayName("미선택 Route의 모든 Step은 LOCKED다")
        void showsAllStepsLockedBeforeSelection() {
            QuestRouteResult.Route route = queryService.route(routeId());

            assertThat(route.playerProgress()).isNull();
            assertThat(route.steps())
                    .extracting(QuestRouteResult.Step::state)
                    .containsOnly(QuestRouteStepState.LOCKED.name());
        }

        @Test
        @DisplayName("선택 후 지난 Step, 준비된 current, 이후 Step을 구분한다")
        void derivesCompletedReadyAndLockedStates() {
            Long routeId = routeId();
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_FIRST_TRACE",
                    COMPLETED_AT
            );
            completeQuest(
                    PLAYER_ID,
                    "Q_RECORD_THREE_TRACES",
                    COMPLETED_AT.plusSeconds(1)
            );
            selectService.select(routeId);
            advanceService.advance(
                    routeId,
                    stepId("RS_RECORD_01_LEAVE_TRACE")
            );

            QuestRouteResult.Route route = queryService.myRoute(routeId);

            assertThat(route.steps()).extracting(QuestRouteResult.Step::state)
                    .containsExactly(
                            QuestRouteStepState.COMPLETED.name(),
                            QuestRouteStepState.READY_TO_ADVANCE.name(),
                            QuestRouteStepState.LOCKED.name()
                    );
            assertThat(queryService.myStep(
                    routeId,
                    stepId("RS_RECORD_02_CONNECT_TRACES")
            ).step().state()).isEqualTo(
                    QuestRouteStepState.READY_TO_ADVANCE.name()
            );
        }
    }

    private Long routeId() {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM quest_routes WHERE code = 'ROUTE_RECORD_START'",
                Long.class
        );
    }

    private Long stepId(String stepCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT step.id
                FROM quest_route_steps step
                JOIN quest_routes route ON route.id = step.route_id
                WHERE route.code = 'ROUTE_RECORD_START'
                  AND step.step_code = ?
                """,
                Long.class,
                stepCode
        );
    }

    private void completeQuest(
            Long playerId,
            String questCode,
            Instant completedAt
    ) {
        Long questId = jdbcTemplate.queryForObject(
                "SELECT id FROM quests WHERE code = ?",
                Long.class,
                questCode
        );
        jdbcTemplate.update("""
                INSERT INTO quest_acceptances (
                    period_start,
                    period_end,
                    accepted_at,
                    period_key,
                    progress_value,
                    completed_at,
                    goal_reached_at,
                    created_at,
                    guild_id,
                    party_id,
                    player_id,
                    quest_id,
                    updated_at,
                    version,
                    idempotency_key,
                    status
                ) VALUES (?, ?, ?, NULL, 1, ?, ?, ?, NULL, NULL, ?, ?, ?, 0, NULL, 'COMPLETED')
                """,
                LocalDate.of(1970, 1, 1),
                LocalDate.of(9999, 12, 31),
                completedAt.minusSeconds(1),
                completedAt,
                completedAt,
                completedAt,
                playerId,
                questId,
                completedAt
        );
    }

    private void addOptionalQuestToFirstStep() {
        jdbcTemplate.update("""
                INSERT INTO quest_route_step_quests (
                    step_id,
                    quest_id,
                    requirement_type
                )
                SELECT ?, quest.id, 'OPTIONAL'
                FROM quests quest
                WHERE quest.code = 'Q_RECORD_THREE_TRACES'
                """, stepId("RS_RECORD_01_LEAVE_TRACE"));
    }

    private Long insertAnotherRoute() {
        jdbcTemplate.update("""
                INSERT INTO quest_routes (
                    code,
                    definition_version,
                    title,
                    description,
                    primary_role_template_code,
                    created_at,
                    updated_at
                ) VALUES (
                    'ROUTE_TEST_SECOND',
                    1,
                    '두 번째 방향',
                    NULL,
                    NULL,
                    CURRENT_TIMESTAMP(6),
                    CURRENT_TIMESTAMP(6)
                )
                """);
        Long routeId = jdbcTemplate.queryForObject(
                "SELECT id FROM quest_routes WHERE code = 'ROUTE_TEST_SECOND'",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO quest_route_steps (
                    route_id,
                    step_code,
                    step_order,
                    title,
                    description,
                    criterion_type,
                    required_evidence_count,
                    user_advance_required,
                    retroactive_evidence_allowed,
                    skip_allowed
                ) VALUES (?, 'ROUTE_TEST_SECOND_STEP', 1, '두 번째 단계', NULL,
                    'QUEST_COMPLETION_SET', 1, b'1', b'1', b'0')
                """, routeId);
        Long stepId = jdbcTemplate.queryForObject(
                "SELECT id FROM quest_route_steps WHERE route_id = ?",
                Long.class,
                routeId
        );
        jdbcTemplate.update("""
                INSERT INTO quest_route_step_quests (
                    step_id,
                    quest_id,
                    requirement_type
                )
                SELECT ?, quest.id, 'REQUIRED'
                FROM quests quest
                WHERE quest.code = 'Q_RECORD_FIRST_TRACE'
                """, stepId);
        return routeId;
    }

    private void deleteTestRoute() {
        jdbcTemplate.update("""
                DELETE link
                FROM quest_route_step_quests link
                JOIN quest_route_steps step ON step.id = link.step_id
                JOIN quest_routes route ON route.id = step.route_id
                WHERE route.code = 'ROUTE_TEST_SECOND'
                """);
        jdbcTemplate.update("""
                DELETE step
                FROM quest_route_steps step
                JOIN quest_routes route ON route.id = step.route_id
                WHERE route.code = 'ROUTE_TEST_SECOND'
                """);
        jdbcTemplate.update(
                "DELETE FROM quest_routes WHERE code = 'ROUTE_TEST_SECOND'"
        );
    }

    private long acceptanceCount(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_acceptances WHERE player_id = ?",
                Long.class,
                playerId
        );
    }

    private long playerRouteCount(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_quest_routes WHERE player_id = ?",
                Long.class,
                playerId
        );
    }

    private List<String> playerRouteStatuses(Long playerId) {
        return jdbcTemplate.queryForList(
                "SELECT status FROM player_quest_routes WHERE player_id = ?",
                String.class,
                playerId
        );
    }

    private Long currentStepId(Long playerId, Long routeId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT current_step_id
                FROM player_quest_routes
                WHERE player_id = ? AND route_id = ?
                """,
                Long.class,
                playerId,
                routeId
        );
    }

    private List<String> runConcurrently(ConcurrentAction action)
            throws Exception {
        return runConcurrently(action, action);
    }

    private List<String> runConcurrently(
            ConcurrentAction firstAction,
            ConcurrentAction secondAction
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<String> first = executor.submit(() -> run(
                    firstAction,
                    ready,
                    start
            ));
            Future<String> second = executor.submit(() -> run(
                    secondAction,
                    ready,
                    start
            ));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }
    }

    private String run(
            ConcurrentAction action,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        start.await();
        return action.run();
    }

    private void assertRouteError(Runnable action, QuestError error) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }

    @FunctionalInterface
    private interface ConcurrentAction {
        String run() throws Exception;
    }
}
