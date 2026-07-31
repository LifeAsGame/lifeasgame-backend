package online.lifeasgame.quest.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.application.automation.QuestProgressStore;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("동시 중복도 row lock으로 Completed Event를 한 번만 남긴다")
    void appliesConcurrentDuplicateExactlyOnce() throws Exception {
        accept(QuestCode.Q_RECOVERY_REST_TEN);
        clock.set(ACCEPTED_AT.plusSeconds(60));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<QuestResult.Acceptance> first = executor.submit(() -> {
                ready.countDown();
                start.await();
                return check(QuestCode.Q_RECOVERY_REST_TEN);
            });
            Future<QuestResult.Acceptance> second = executor.submit(() -> {
                ready.countDown();
                start.await();
                return check(QuestCode.Q_RECOVERY_REST_TEN);
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

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
    @DisplayName("cancel 후 같은 날 재수락은 acceptedAt으로 새 attempt가 된다")
    void separatesCanceledAndReacceptedAttempt() {
        accept(QuestCode.Q_GROWTH_ONE_FOCUS);
        clock.set(ACCEPTED_AT.plusSeconds(60));
        doThrow(new RuntimeException("stop before completion"))
                .doCallRealMethod()
                .when(completionService)
                .completeForPlayer(eq(PLAYER_ID), anyLong());
        assertThatThrownBy(
                () -> check(QuestCode.Q_GROWTH_ONE_FOCUS)
        ).isInstanceOf(RuntimeException.class);

        questService.cancel(
                PLAYER_ID,
                new QuestCommand.Cancel(
                        QuestCode.Q_GROWTH_ONE_FOCUS.value(),
                        "new manual attempt"
                )
        );
        Instant restartedAt = ACCEPTED_AT.plusSeconds(120);
        clock.set(restartedAt);
        QuestResult.Acceptance restarted =
                questService.accept(
                        PLAYER_ID,
                        new QuestCommand.Accept(
                                QuestCode.Q_GROWTH_ONE_FOCUS.value(),
                                null,
                                null
                        )
                );
        clock.set(restartedAt.plusSeconds(60));

        QuestResult.Acceptance completed = check(
                QuestCode.Q_GROWTH_ONE_FOCUS
        );

        assertThat(restarted.id()).isEqualTo(completed.id());
        assertThat(restarted.acceptedAt()).isEqualTo(restartedAt);
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
                QuestEventType.QUEST_PROGRESS.name(),
                QuestEventType.QUEST_GOAL_REACHED.name(),
                QuestEventType.QUEST_COMPLETED.name()
        );
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

        void set(Instant instant) {
            current = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
