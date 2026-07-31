package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.application.automation.QuestProgressStore;
import online.lifeasgame.quest.application.automation.QuestSignal;
import online.lifeasgame.quest.application.automation.QuestSignalProcessingService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(
        QuestManualCheckIntegrationTest.MutableClockConfiguration.class
)
@DisplayName("QuestManualCheck MySQL 통합")
class QuestManualCheckIntegrationTest {

    private static final Long PLAYER_ID = 217001L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-31T01:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_manual_check")
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
    private QuestService questService;

    @Autowired
    private QuestManualCheckService manualCheckService;

    @MockitoSpyBean
    private QuestAcceptanceCompletionService completionService;

    @MockitoSpyBean
    private QuestSignalProcessingService signalProcessingService;

    @MockitoBean
    private QuestProgressStore questProgressStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock.set(ACCEPTED_AT);
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id = ?",
                PLAYER_ID
        );
    }

    @Test
    @DisplayName("순차 중복은 Receipt와 세 Event를 한 번씩 남긴다")
    void appliesSequentialDuplicateExactlyOnce() {
        accept(QuestCode.Q_GROWTH_ONE_FOCUS);
        Instant checkedAt = ACCEPTED_AT.plusSeconds(60);
        clock.set(checkedAt);

        QuestResult.Acceptance first = check(
                QuestCode.Q_GROWTH_ONE_FOCUS
        );
        QuestResult.Acceptance replay = check(
                QuestCode.Q_GROWTH_ONE_FOCUS
        );

        assertThat(first.status()).isEqualTo(
                QuestStatus.COMPLETED.name()
        );
        assertThat(replay.completedAt()).isEqualTo(first.completedAt());
        assertThat(first.completedAt()).isEqualTo(checkedAt);
        assertThat(progress(QuestCode.Q_GROWTH_ONE_FOCUS)).isEqualTo(25);
        assertThat(status(QuestCode.Q_GROWTH_ONE_FOCUS))
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(correlations()).containsExactly(
                correlation(first.id(), first.acceptedAt())
        );
        assertThat(eventTypes()).containsExactly(
                QuestEventType.QUEST_PROGRESS.name(),
                QuestEventType.QUEST_GOAL_REACHED.name(),
                QuestEventType.QUEST_COMPLETED.name()
        );
    }

    @Test
    @DisplayName("서로 다른 checkedAt의 동시 중복도 exact-once다")
    void appliesConcurrentDuplicateExactlyOnce() throws Exception {
        accept(QuestCode.Q_RECOVERY_REST_TEN);
        Instant firstCheckedAt = ACCEPTED_AT.plusSeconds(60);
        Instant secondCheckedAt = ACCEPTED_AT.plusSeconds(61);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch processingReady = new CountDownLatch(2);
        CountDownLatch process = new CountDownLatch(1);
        doAnswer(invocation -> {
            processingReady.countDown();
            assertThat(process.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(signalProcessingService).process(any(QuestSignal.class));
        try {
            Future<QuestResult.Acceptance> first = submitCheck(
                    executor,
                    ready,
                    start,
                    QuestCode.Q_RECOVERY_REST_TEN,
                    firstCheckedAt
            );
            Future<QuestResult.Acceptance> second = submitCheck(
                    executor,
                    ready,
                    start,
                    QuestCode.Q_RECOVERY_REST_TEN,
                    secondCheckedAt
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(processingReady.await(5, TimeUnit.SECONDS))
                    .isTrue();
            process.countDown();

            assertThat(List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            )).extracting(QuestResult.Acceptance::status)
                    .containsOnly(QuestStatus.COMPLETED.name());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }

        assertThat(progress(QuestCode.Q_RECOVERY_REST_TEN)).isEqualTo(10);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(eventTypes()).containsExactly(
                QuestEventType.QUEST_PROGRESS.name(),
                QuestEventType.QUEST_GOAL_REACHED.name(),
                QuestEventType.QUEST_COMPLETED.name()
        );
    }

    @Test
    @DisplayName("Signal 후 completion 실패는 GOAL_REACHED에서 재시도된다")
    void retriesCompletionAfterSignalCheckpoint() {
        accept(QuestCode.Q_GROWTH_ONE_FOCUS);
        clock.set(ACCEPTED_AT.plusSeconds(60));
        doThrow(new RuntimeException("forced completion failure"))
                .doCallRealMethod()
                .when(completionService)
                .completeForPlayer(eq(PLAYER_ID), anyLong());

        assertThatThrownBy(
                () -> check(QuestCode.Q_GROWTH_ONE_FOCUS)
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("forced completion failure");

        assertThat(status(QuestCode.Q_GROWTH_ONE_FOCUS))
                .isEqualTo(QuestStatus.GOAL_REACHED.name());
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(eventTypes()).containsExactly(
                QuestEventType.QUEST_PROGRESS.name(),
                QuestEventType.QUEST_GOAL_REACHED.name()
        );

        QuestResult.Acceptance retried = check(
                QuestCode.Q_GROWTH_ONE_FOCUS
        );

        assertThat(retried.status()).isEqualTo(
                QuestStatus.COMPLETED.name()
        );
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(eventTypes()).containsExactly(
                QuestEventType.QUEST_PROGRESS.name(),
                QuestEventType.QUEST_GOAL_REACHED.name(),
                QuestEventType.QUEST_COMPLETED.name()
        );
    }

    @Test
    @DisplayName("Signal 직전 cancel/reaccept된 새 attempt를 변경하지 않는다")
    void isolatesCanceledAndReacceptedAttemptBeforeProcessing()
            throws Exception {
        accept(QuestCode.Q_GROWTH_ONE_FOCUS);
        Instant oldAttemptCheckedAt = ACCEPTED_AT.plusSeconds(180);
        Instant restartedAt = ACCEPTED_AT.plusSeconds(120);
        CountDownLatch processingEntered = new CountDownLatch(1);
        CountDownLatch process = new CountDownLatch(1);
        doAnswer(invocation -> {
            processingEntered.countDown();
            assertThat(process.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(signalProcessingService).process(any(QuestSignal.class));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<QuestResult.Acceptance> oldAttempt =
                    executor.submit(() -> checkWithThreadClock(
                            QuestCode.Q_GROWTH_ONE_FOCUS,
                            oldAttemptCheckedAt
                    ));
            assertThat(processingEntered.await(5, TimeUnit.SECONDS))
                    .isTrue();

            clock.set(restartedAt);
            questService.cancel(
                    PLAYER_ID,
                    new QuestCommand.Cancel(
                            QuestCode.Q_GROWTH_ONE_FOCUS.value(),
                            "processing race"
                    )
            );
            QuestResult.Acceptance restarted = questService.accept(
                    PLAYER_ID,
                    new QuestCommand.Accept(
                            QuestCode.Q_GROWTH_ONE_FOCUS.value(),
                            null,
                            null
                    )
            );
            assertThat(oldAttemptCheckedAt).isAfter(restartedAt);
            assertThat(restarted.acceptedAt()).isEqualTo(restartedAt);

            process.countDown();
            assertThatThrownBy(
                    () -> oldAttempt.get(20, TimeUnit.SECONDS)
            ).isInstanceOfSatisfying(
                    ExecutionException.class,
                    exception -> assertThat(exception.getCause())
                            .isInstanceOfSatisfying(
                                    DomainException.class,
                                    cause -> assertThat(
                                            cause.getErrorCode()
                                    ).isEqualTo(
                                            QuestError
                                                    .QUEST_ACCEPTANCE_NOT_FOUND
                                    )
                            )
            );

            assertThat(status(QuestCode.Q_GROWTH_ONE_FOCUS))
                    .isEqualTo(QuestStatus.IN_PROGRESS.name());
            assertThat(progress(QuestCode.Q_GROWTH_ONE_FOCUS)).isZero();
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(correlations()).containsExactly(
                    correlation(restarted.id(), ACCEPTED_AT)
            );
            assertThat(eventTypes()).isEmpty();

            Instant newAttemptCheckedAt =
                    restartedAt.plusSeconds(120);
            clock.set(newAttemptCheckedAt);
            QuestResult.Acceptance completed = check(
                    QuestCode.Q_GROWTH_ONE_FOCUS
            );

            assertThat(completed.id()).isEqualTo(restarted.id());
            assertThat(completed.acceptedAt()).isEqualTo(restartedAt);
            assertThat(completed.status()).isEqualTo(
                    QuestStatus.COMPLETED.name()
            );
            assertThat(receiptCount()).isEqualTo(2);
            assertThat(correlations()).containsExactly(
                    correlation(restarted.id(), ACCEPTED_AT),
                    correlation(restarted.id(), restartedAt)
            );
            assertThat(eventTypes()).containsExactly(
                    QuestEventType.QUEST_PROGRESS.name(),
                    QuestEventType.QUEST_GOAL_REACHED.name(),
                    QuestEventType.QUEST_COMPLETED.name()
            );
        } finally {
            process.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Asia/Seoul 자정 경계와 Clock timestamp를 결정적으로 적용한다")
    void usesPlayerTimezoneAndClock() {
        Instant beforeLocalMidnight =
                Instant.parse("2026-07-31T14:59:00Z");
        clock.set(beforeLocalMidnight);
        accept(QuestCode.Q_RECOVERY_REST_TEN);
        clock.set(Instant.parse("2026-07-31T15:01:00Z"));

        assertThatThrownBy(
                () -> check(QuestCode.Q_RECOVERY_REST_TEN)
        ).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(QuestError.QUEST_ACCEPTANCE_NOT_FOUND)
        );
        assertThat(receiptCount()).isZero();

        QuestResult.Acceptance current = questService.accept(
                PLAYER_ID,
                new QuestCommand.Accept(
                        QuestCode.Q_RECOVERY_REST_TEN.value(),
                        null,
                        null
                )
        );
        Instant checkedAt = Instant.parse("2026-07-31T15:02:00Z");
        clock.set(checkedAt);
        QuestResult.Acceptance completed = check(
                QuestCode.Q_RECOVERY_REST_TEN
        );

        assertThat(current.periodStart()).isEqualTo(
                java.time.LocalDate.of(2026, 8, 1)
        );
        assertThat(completed.completedAt()).isEqualTo(checkedAt);
    }

    private void accept(QuestCode questCode) {
        questService.accept(
                PLAYER_ID,
                new QuestCommand.Accept(
                        questCode.value(),
                        null,
                        null
                )
        );
    }

    private QuestResult.Acceptance check(QuestCode questCode) {
        return manualCheckService.check(
                PLAYER_ID,
                new QuestCommand.ManualCheck(questCode.value())
        );
    }

    private Future<QuestResult.Acceptance> submitCheck(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            QuestCode questCode,
            Instant checkedAt
    ) {
        return executor.submit(() -> {
            clock.setForCurrentThread(checkedAt);
            try {
                ready.countDown();
                start.await();
                return check(questCode);
            } finally {
                clock.clearCurrentThread();
            }
        });
    }

    private QuestResult.Acceptance checkWithThreadClock(
            QuestCode questCode,
            Instant checkedAt
    ) {
        clock.setForCurrentThread(checkedAt);
        try {
            return check(questCode);
        } finally {
            clock.clearCurrentThread();
        }
    }

    private int receiptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_signal_receipts WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private List<String> correlations() {
        return jdbcTemplate.queryForList(
                """
                SELECT correlation_id
                FROM quest_signal_receipts
                WHERE player_id = ?
                ORDER BY id
                """,
                String.class,
                PLAYER_ID
        );
    }

    private int progress(QuestCode questCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT acceptance.progress_value
                FROM quest_acceptances acceptance
                JOIN quests quest ON quest.id = acceptance.quest_id
                WHERE acceptance.player_id = ?
                  AND quest.code = ?
                ORDER BY acceptance.id DESC
                LIMIT 1
                """,
                Integer.class,
                PLAYER_ID,
                questCode.value()
        );
    }

    private String status(QuestCode questCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT acceptance.status
                FROM quest_acceptances acceptance
                JOIN quests quest ON quest.id = acceptance.quest_id
                WHERE acceptance.player_id = ?
                  AND quest.code = ?
                ORDER BY acceptance.id DESC
                LIMIT 1
                """,
                String.class,
                PLAYER_ID,
                questCode.value()
        );
    }

    private List<String> eventTypes() {
        return jdbcTemplate.queryForList(
                """
                SELECT JSON_UNQUOTE(JSON_EXTRACT(payload, '$.type'))
                FROM outbox_events
                WHERE event_type = 'quest.event.v1'
                  AND JSON_EXTRACT(payload, '$.playerId') = ?
                ORDER BY id
                """,
                String.class,
                PLAYER_ID
        );
    }

    private String correlation(Long acceptanceId, Instant acceptedAt) {
        return "manual-check:acceptance:%d:accepted-at:%d".formatted(
                acceptanceId,
                acceptedAt.toEpochMilli()
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock manualCheckClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant current = ACCEPTED_AT;
        private final ThreadLocal<Instant> currentThread =
                new ThreadLocal<>();

        void set(Instant instant) {
            current = instant;
        }

        void setForCurrentThread(Instant instant) {
            currentThread.set(instant);
        }

        void clearCurrentThread() {
            currentThread.remove();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            Instant threadInstant = currentThread.get();
            return threadInstant == null ? current : threadInstant;
        }
    }
}
