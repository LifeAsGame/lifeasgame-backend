package online.lifeasgame.quest.application;

import online.lifeasgame.adminaudit.domain.error.AdminAuditError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.quest.application.command.AdminQuestAcceptanceOverrideCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.QuestCategory;
import online.lifeasgame.quest.domain.QuestCompletionPolicy;
import online.lifeasgame.quest.domain.QuestRepeatRule;
import online.lifeasgame.quest.domain.QuestReward;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.QuestTarget;
import online.lifeasgame.quest.domain.QuestTargetType;
import online.lifeasgame.quest.domain.QuestTitle;
import online.lifeasgame.quest.domain.RewardStats;
import online.lifeasgame.quest.domain.TimePeriod;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Admin Quest Acceptance override MySQL transaction contract")
class AdminQuestAcceptanceOverrideIntegrationTest {

    private static final long ADMIN_ID = 9308L;
    private static final long PLAYER_ID = 19308L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-08-25T00:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_admin_quest")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame")
                    .withCommand("--log-bin-trust-function-creators=1");

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
    private AdminQuestAcceptanceOverrideService overrideService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private QuestAcceptanceRepository acceptanceRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    private int seedSequence;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TRIGGER IF EXISTS force_admin_quest_audit_failure");
        jdbc.update("DELETE FROM admin_audit_events");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM quest_acceptances");
        jdbc.update("DELETE FROM quests WHERE code LIKE 'quest:test:admin-override:%'");
        jdbc.update("DELETE FROM users WHERE id = ?", ADMIN_ID);
        jdbc.update("""
                INSERT INTO users (
                    id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'ACTIVE', 'ADMIN', NOW(6), NOW(6))
                """, ADMIN_ID, "quest-admin@example.com", "hash", "quest-admin");
        jdbc.execute("""
                CREATE TRIGGER force_admin_quest_audit_failure
                BEFORE INSERT ON admin_audit_events
                FOR EACH ROW
                SET NEW.actor_user_id = IF(
                    NEW.correlation_id = 'force-audit-failure',
                    999999999,
                    NEW.actor_user_id
                )
                """);
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(ADMIN_ID);
    }

    @Test
    @DisplayName("progress는 Acceptance, event, Audit을 한 transaction으로 commit한다")
    void commitsProgressAtomically() {
        long acceptanceId = seedAcceptance();

        QuestResult.Acceptance result = overrideService.adjustProgress(
                progress(acceptanceId, 2, "progress-success", "request-progress")
        );

        assertThat(result.progressValue()).isEqualTo(2);
        assertThat(progress(acceptanceId)).isEqualTo(2);
        assertThat(outboxTypes()).containsExactly("QUEST_PROGRESS");
        assertThat(auditRow("QUEST_ACCEPTANCE_PROGRESS_ADJUST"))
                .containsExactly(
                        ADMIN_ID,
                        "QUEST_ACCEPTANCE_PROGRESS_ADJUST",
                        "QUEST_ACCEPTANCE",
                        Long.toString(acceptanceId),
                        "CASE-308",
                        "SUCCESS",
                        "request-progress",
                        "progress-success"
                );
    }

    @Test
    @DisplayName("status는 domain transition event와 Audit을 함께 commit한다")
    void commitsStatusAtomically() {
        long acceptanceId = seedAcceptance();

        QuestResult.Acceptance result = overrideService.changeStatus(
                status(
                        acceptanceId,
                        "GOAL_REACHED",
                        "status-success",
                        "request-status"
                )
        );

        assertThat(result.status()).isEqualTo("GOAL_REACHED");
        assertThat(status(acceptanceId)).isEqualTo("GOAL_REACHED");
        assertThat(outboxTypes()).containsExactly("QUEST_GOAL_REACHED");
        assertThat(auditRow("QUEST_ACCEPTANCE_STATUS_CHANGE"))
                .containsExactly(
                        ADMIN_ID,
                        "QUEST_ACCEPTANCE_STATUS_CHANGE",
                        "QUEST_ACCEPTANCE",
                        Long.toString(acceptanceId),
                        "CASE-308",
                        "SUCCESS",
                        "request-status",
                        "status-success"
                );
    }

    @Test
    @DisplayName("두 action의 sequential duplicate는 각각 한 effect만 commit한다")
    void rejectsSequentialDuplicates() {
        long progressAcceptanceId = seedAcceptance();
        long statusAcceptanceId = seedAcceptance();
        overrideService.adjustProgress(progress(
                progressAcceptanceId,
                2,
                "progress-duplicate",
                "request-progress-1"
        ));
        overrideService.changeStatus(status(
                statusAcceptanceId,
                "GOAL_REACHED",
                "status-duplicate",
                "request-status-1"
        ));

        assertDuplicate(() -> overrideService.adjustProgress(progress(
                progressAcceptanceId,
                2,
                "progress-duplicate",
                "request-progress-2"
        )));
        assertDuplicate(() -> overrideService.changeStatus(status(
                statusAcceptanceId,
                "GOAL_REACHED",
                "status-duplicate",
                "request-status-2"
        )));

        assertThat(progress(progressAcceptanceId)).isEqualTo(2);
        assertThat(status(statusAcceptanceId)).isEqualTo("GOAL_REACHED");
        assertThat(auditCount()).isEqualTo(2);
        assertThat(outboxCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("concurrent same key는 DB uniqueness와 Acceptance lock으로 한 번만 반영된다")
    void rejectsConcurrentDuplicate() throws Exception {
        long acceptanceId = seedAcceptance();

        List<Outcome> outcomes = concurrently(
                () -> overrideService.adjustProgress(progress(
                        acceptanceId,
                        2,
                        "concurrent-duplicate",
                        "request-concurrent-1"
                )),
                () -> overrideService.adjustProgress(progress(
                        acceptanceId,
                        2,
                        "concurrent-duplicate",
                        "request-concurrent-2"
                ))
        );

        assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                .singleElement()
                .satisfies(outcome -> assertDuplicate(outcome.failure()));
        assertThat(progress(acceptanceId)).isEqualTo(2);
        assertThat(auditCount()).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("different concurrent delta는 row lock으로 직렬화되어 lost update가 없다")
    void serializesConflictingOverrides() throws Exception {
        long acceptanceId = seedAcceptance();

        List<Outcome> outcomes = concurrently(
                () -> overrideService.adjustProgress(progress(
                        acceptanceId,
                        2,
                        "concurrent-delta-2",
                        "request-delta-2"
                )),
                () -> overrideService.adjustProgress(progress(
                        acceptanceId,
                        3,
                        "concurrent-delta-3",
                        "request-delta-3"
                ))
        );

        assertThat(outcomes).allMatch(Outcome::success);
        assertThat(progress(acceptanceId)).isEqualTo(5);
        assertThat(auditCount()).isEqualTo(2);
        assertThat(outboxCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Audit failure는 progress/status와 outbox를 모두 rollback한다")
    void rollsBackAuditFailure() {
        long progressAcceptanceId = seedAcceptance();
        long statusAcceptanceId = seedAcceptance();

        assertThatThrownBy(() -> overrideService.adjustProgress(progress(
                progressAcceptanceId,
                2,
                "audit-progress-failure",
                "force-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> overrideService.changeStatus(status(
                statusAcceptanceId,
                "GOAL_REACHED",
                "audit-status-failure",
                "force-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(progress(progressAcceptanceId)).isZero();
        assertThat(status(statusAcceptanceId)).isEqualTo("IN_PROGRESS");
        assertThat(auditCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    @Test
    @DisplayName("illegal status transition은 key를 소비하지 않아 domain 상태 보정 후 retry할 수 있다")
    void retriesAfterIllegalTransition() {
        long acceptanceId = seedAcceptance();
        String retryKey = "retry-illegal-status";

        assertThatThrownBy(() -> overrideService.changeStatus(status(
                acceptanceId,
                "COMPLETED",
                retryKey,
                "request-illegal"
        ))).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        QuestError.QUEST_ACCEPTANCE_COMPLETION_NOT_ALLOWED
                )
        );
        assertThat(auditCount()).isZero();
        assertThat(outboxCount()).isZero();

        overrideService.changeStatus(status(
                acceptanceId,
                "GOAL_REACHED",
                "prepare-goal-status",
                "request-goal"
        ));
        overrideService.changeStatus(status(
                acceptanceId,
                "COMPLETED",
                retryKey,
                "request-retry"
        ));

        assertThat(status(acceptanceId)).isEqualTo("COMPLETED");
        assertThat(auditCount()).isEqualTo(2);
        assertThat(outboxTypes()).containsExactly(
                "QUEST_GOAL_REACHED",
                "QUEST_COMPLETED"
        );
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events WHERE idempotency_key = ?",
                Integer.class,
                retryKey
        )).isEqualTo(1);
    }

    private long seedAcceptance() {
        Quest quest = questRepository.save(Quest.create(
                "quest:test:admin-override:" + (++seedSequence),
                QuestCategory.MAIN,
                QuestTitle.of("Admin override test"),
                "Admin Quest Acceptance override integration test",
                QuestTarget.of(QuestTargetType.COUNT, 10),
                QuestReward.of(0, RewardStats.empty()),
                QuestRepeatRule.NONE,
                QuestCompletionPolicy.USER_CONFIRM,
                null
        ));
        QuestAcceptance acceptance = QuestAcceptance.start(
                quest.getId(),
                PLAYER_ID,
                TimePeriod.forever(),
                ACCEPTED_AT,
                null
        );
        return acceptanceRepository.save(acceptance).getId();
    }

    private AdminQuestAcceptanceOverrideCommand.AdjustProgress progress(
            long acceptanceId,
            int delta,
            String key,
            String correlationId
    ) {
        return new AdminQuestAcceptanceOverrideCommand.AdjustProgress(
                acceptanceId,
                delta,
                "CASE-308",
                key,
                correlationId
        );
    }

    private AdminQuestAcceptanceOverrideCommand.ChangeStatus status(
            long acceptanceId,
            String status,
            String key,
            String correlationId
    ) {
        return new AdminQuestAcceptanceOverrideCommand.ChangeStatus(
                acceptanceId,
                status,
                "CASE-308",
                key,
                correlationId
        );
    }

    private List<Outcome> concurrently(Runnable first, Runnable second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstResult = executor.submit(
                    () -> invoke(ready, start, first)
            );
            Future<Outcome> secondResult = executor.submit(
                    () -> invoke(ready, start, second)
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Outcome invoke(
            CountDownLatch ready,
            CountDownLatch start,
            Runnable command
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new IllegalStateException("start timeout"));
            }
            command.run();
            return new Outcome(true, null);
        } catch (Throwable exception) {
            return new Outcome(false, exception);
        }
    }

    private void assertDuplicate(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        this::assertDuplicate
                );
    }

    private void assertDuplicate(Throwable failure) {
        assertThat(failure).isInstanceOf(DomainException.class);
        assertThat(((DomainException) failure).getErrorCode()).isEqualTo(
                AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY
        );
    }

    private int progress(long acceptanceId) {
        return jdbc.queryForObject(
                "SELECT progress_value FROM quest_acceptances WHERE id = ?",
                Integer.class,
                acceptanceId
        );
    }

    private String status(long acceptanceId) {
        return jdbc.queryForObject(
                "SELECT status FROM quest_acceptances WHERE id = ?",
                String.class,
                acceptanceId
        );
    }

    private int auditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events",
                Integer.class
        );
    }

    private int outboxCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Integer.class
        );
    }

    private List<String> outboxTypes() {
        return jdbc.queryForList("""
                SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.type'))
                FROM outbox_events
                ORDER BY id
                """, String.class);
    }

    private List<Object> auditRow(String action) {
        return jdbc.queryForObject("""
                SELECT actor_user_id, action, target_type, target_id,
                       reason, result, correlation_id, idempotency_key
                FROM admin_audit_events
                WHERE action = ?
                """, (result, row) -> List.of(
                result.getLong("actor_user_id"),
                result.getString("action"),
                result.getString("target_type"),
                result.getString("target_id"),
                result.getString("reason"),
                result.getString("result"),
                result.getString("correlation_id"),
                result.getString("idempotency_key")
        ), action);
    }

    private record Outcome(boolean success, Throwable failure) {
    }
}
