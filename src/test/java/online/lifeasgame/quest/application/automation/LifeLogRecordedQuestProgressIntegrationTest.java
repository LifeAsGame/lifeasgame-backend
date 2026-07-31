package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.application.OutboxRelayService;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.trigger.LifeLogRecordedQuestTrigger;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(
        LifeLogRecordedQuestProgressIntegrationTest
                .MutableClockConfiguration.class
)
@DisplayName("LifeLogRecorded Level 1 Quest MySQL 진행")
class LifeLogRecordedQuestProgressIntegrationTest {

    private static final Long PLAYER_ID = 215001L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-30T01:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_lifelog_quest")
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
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private LifeLogRecordedQuestTrigger trigger;

    @Autowired
    private QuestSignalProcessingService processingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private QuestProgressStore questProgressStore;

    private TransactionTemplate transactionTemplate;

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
        outboxProperties.setBatchSize(100);
        outboxProperties.setMaxAttempts(3);
        outboxProperties.setRetryDelayMs(0);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Acceptance가 없던 Fact는 나중에 수락해도 replay로 진행하지 않는다")
    void neverReplaysFactSeenWithoutAcceptance() {
        LifeLogRecorded firstDelivery = regular(
                "215-no-acceptance-a",
                215100L,
                ACCEPTED_AT.plusSeconds(1)
        );

        appendAndRelay(firstDelivery);

        assertThat(acceptanceCount()).isZero();
        assertThat(receiptCount()).isEqualTo(2);

        accept(QuestCode.Q_RECORD_THREE_TRACES);
        appendAndRelay(regular(
                "215-no-acceptance-b",
                firstDelivery.lifeLogId(),
                firstDelivery.occurredAt()
        ));

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isZero();
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("acceptedAt 이전 Fact는 다른 eventId 재전달까지 영구 무시한다")
    void permanentlyIgnoresPreAcceptanceFact() {
        accept(QuestCode.Q_RECORD_THREE_TRACES);
        LifeLogRecorded beforeAcceptance = regular(
                "215-pre-accept-a",
                215101L,
                ACCEPTED_AT.minusSeconds(1)
        );

        appendAndRelay(beforeAcceptance);
        appendAndRelay(regular(
                "215-pre-accept-b",
                beforeAcceptance.lifeLogId(),
                beforeAcceptance.occurredAt()
        ));

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isZero();
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("취소 후 같은 period 재수락은 기존 row를 초기화하고 old Fact를 이월하지 않는다")
    void restartsCanceledAcceptanceWithoutCarryingOldFacts() {
        accept(QuestCode.Q_RECORD_THREE_TRACES);
        appendAndRelay(regular(
                "215-reaccept-before-cancel",
                215108L,
                ACCEPTED_AT.plusSeconds(1)
        ));
        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        LifeLogRecorded oldFact = regular(
                "215-reaccept-old-a",
                215109L,
                ACCEPTED_AT.plusSeconds(2)
        );

        questService.cancel(
                PLAYER_ID,
                new QuestCommand.Cancel(
                        QuestCode.Q_RECORD_THREE_TRACES.value(),
                        "restart integration test"
                )
        );
        Instant restartedAt = ACCEPTED_AT.plusSeconds(10);
        clock.set(restartedAt);
        accept(QuestCode.Q_RECORD_THREE_TRACES);

        assertThat(acceptanceCount()).isEqualTo(1);
        assertThat(acceptedAt(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(restartedAt);
        assertThat(status(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(QuestStatus.IN_PROGRESS.name());
        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isZero();

        appendAndRelay(oldFact);

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isZero();
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(2);

        appendAndRelay(regular(
                "215-reaccept-new",
                215110L,
                restartedAt.plusSeconds(1)
        ));

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        assertThat(status(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(QuestStatus.IN_PROGRESS.name());

        appendAndRelay(regular(
                "215-reaccept-old-b",
                oldFact.lifeLogId(),
                oldFact.occurredAt()
        ));

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(3);
    }

    @Test
    @DisplayName("한 LifeLog가 첫 기록과 세 기록 Quest를 동시에 진행한다")
    void progressesFirstAndThreeTracesTogether() {
        accept(QuestCode.Q_RECORD_FIRST_TRACE);
        accept(QuestCode.Q_RECORD_THREE_TRACES);

        appendAndRelay(regular(
                "215-simultaneous",
                215102L,
                ACCEPTED_AT.plusSeconds(1)
        ));

        assertThat(progress(QuestCode.Q_RECORD_FIRST_TRACE)).isEqualTo(1);
        assertThat(status(QuestCode.Q_RECORD_FIRST_TRACE))
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        assertThat(status(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(QuestStatus.IN_PROGRESS.name());
    }

    @Test
    @DisplayName("같은 global lifeLogId의 다른 eventId는 durable Receipt replay다")
    void replaysSameLifeLogWithDifferentEventId() {
        accept(QuestCode.Q_RECORD_THREE_TRACES);
        LifeLogRecorded first = regular(
                "215-replay-a",
                215103L,
                ACCEPTED_AT.plusSeconds(1)
        );

        appendAndRelay(first);
        appendAndRelay(regular(
                "215-replay-b",
                first.lifeLogId(),
                first.occurredAt()
        ));

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(1);
        assertThat(correlations(QuestCode.Q_RECORD_THREE_TRACES))
                .containsExactly("lifelog:" + first.lifeLogId());
    }

    @Test
    @DisplayName("동일 LifeLog Signal 동시 중복은 한 번만 적용한다")
    void appliesConcurrentDuplicateOnce() throws Exception {
        accept(QuestCode.Q_RECORD_THREE_TRACES);
        QuestSignal signal = trigger.translate(regular(
                        "215-concurrent",
                        215104L,
                        ACCEPTED_AT.plusSeconds(1)
                )).stream()
                .filter(candidate -> candidate.questCode()
                        == QuestCode.Q_RECORD_THREE_TRACES)
                .findFirst()
                .orElseThrow();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<QuestSignalProcessingResult> first =
                    submit(executor, ready, start, signal);
            Future<QuestSignalProcessingResult> second =
                    submit(executor, ready, start, signal);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)
            )).extracting(QuestSignalProcessingResult::outcome)
                    .containsExactlyInAnyOrder(
                            QuestSignalProcessingResult.Outcome.APPLIED,
                            QuestSignalProcessingResult.Outcome.REPLAYED
                    );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                    .isTrue();
        }

        assertThat(progress(QuestCode.Q_RECORD_THREE_TRACES)).isEqualTo(1);
        assertThat(receiptCount(QuestCode.Q_RECORD_THREE_TRACES))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("weekly는 FULL exact period만 인정하고 QUICK과 다른 period는 무시한다")
    void requiresFullWeeklyLookbackWithExactPeriod() {
        accept(QuestCode.Q_RECORD_WEEKLY_LOOKBACK);
        assertThat(periodKey(QuestCode.Q_RECORD_WEEKLY_LOOKBACK))
                .isEqualTo("2026-W31");
        assertThat(acceptedAt(QuestCode.Q_RECORD_WEEKLY_LOOKBACK))
                .isEqualTo(ACCEPTED_AT);

        appendAndRelay(weekly(
                "215-weekly-quick",
                215105L,
                LifeLogEntryMode.QUICK,
                "2026-W31",
                ACCEPTED_AT.plusSeconds(1)
        ));
        assertThat(progress(QuestCode.Q_RECORD_WEEKLY_LOOKBACK)).isZero();
        assertThat(receiptCount(QuestCode.Q_RECORD_WEEKLY_LOOKBACK))
                .isZero();

        appendAndRelay(weekly(
                "215-weekly-mismatch",
                215106L,
                LifeLogEntryMode.FULL,
                "2026-W30",
                ACCEPTED_AT.plusSeconds(2)
        ));
        assertThat(progress(QuestCode.Q_RECORD_WEEKLY_LOOKBACK)).isZero();

        appendAndRelay(weekly(
                "215-weekly-exact",
                215107L,
                LifeLogEntryMode.FULL,
                "2026-W31",
                ACCEPTED_AT.plusSeconds(3)
        ));

        assertThat(progress(QuestCode.Q_RECORD_WEEKLY_LOOKBACK)).isEqualTo(1);
        assertThat(status(QuestCode.Q_RECORD_WEEKLY_LOOKBACK))
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(receiptCount(QuestCode.Q_RECORD_WEEKLY_LOOKBACK))
                .isEqualTo(2);
    }

    private Future<QuestSignalProcessingResult> submit(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            QuestSignal signal
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            return processingService.process(signal);
        });
    }

    private void accept(QuestCode questCode) {
        questService.accept(
                PLAYER_ID,
                new QuestCommand.Accept(questCode.value(), null, null)
        );
    }

    private void appendAndRelay(LifeLogRecorded event) {
        transactionTemplate.executeWithoutResult(status ->
                domainEventPublisher.publish(event)
        );
        relayService.relayBatch();
    }

    private LifeLogRecorded regular(
            String eventId,
            Long lifeLogId,
            Instant occurredAt
    ) {
        return event(
                eventId,
                lifeLogId,
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null,
                occurredAt
        );
    }

    private LifeLogRecorded weekly(
            String eventId,
            Long lifeLogId,
            LifeLogEntryMode entryMode,
            String periodKey,
            Instant occurredAt
    ) {
        return event(
                eventId,
                lifeLogId,
                LifeLogSubtype.REFLECTION,
                entryMode,
                LifeLogReflectionScope.WEEKLY_LOOKBACK,
                periodKey,
                occurredAt
        );
    }

    private LifeLogRecorded event(
            String eventId,
            Long lifeLogId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            String periodKey,
            Instant occurredAt
    ) {
        return new LifeLogRecorded(
                eventId,
                LifeLogRecorded.EVENT_TYPE,
                LifeLogRecorded.EVENT_VERSION,
                occurredAt,
                PLAYER_ID,
                lifeLogId,
                1,
                subtype,
                entryMode,
                reflectionScope,
                periodKey,
                null,
                null
        );
    }

    private int acceptanceCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_acceptances WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private int receiptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_signal_receipts WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private int receiptCount(QuestCode questCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM quest_signal_receipts
                WHERE player_id = ?
                  AND quest_code = ?
                """,
                Integer.class,
                PLAYER_ID,
                questCode.value()
        );
    }

    private List<String> correlations(QuestCode questCode) {
        return jdbcTemplate.queryForList(
                """
                SELECT correlation_id
                FROM quest_signal_receipts
                WHERE player_id = ?
                  AND quest_code = ?
                ORDER BY id
                """,
                String.class,
                PLAYER_ID,
                questCode.value()
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

    private String periodKey(QuestCode questCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT acceptance.period_key
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

    private Instant acceptedAt(QuestCode questCode) {
        return jdbcTemplate.queryForObject(
                """
                SELECT acceptance.accepted_at
                FROM quest_acceptances acceptance
                JOIN quests quest ON quest.id = acceptance.quest_id
                WHERE acceptance.player_id = ?
                  AND quest.code = ?
                ORDER BY acceptance.id DESC
                LIMIT 1
                """,
                (resultSet, rowNum) ->
                        resultSet.getObject(
                                "accepted_at",
                                LocalDateTime.class
                        ).toInstant(ZoneOffset.UTC),
                PLAYER_ID,
                questCode.value()
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock lifeLogQuestClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant current = ACCEPTED_AT;

        void set(Instant instant) {
            this.current = instant;
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
