package online.lifeasgame.quest.application.automation;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(QuestSignalProcessingIntegrationTest.EventProbeConfig.class)
@DisplayName("QuestSignalReceipt MySQL 처리")
class QuestSignalProcessingIntegrationTest {

    private static final Long PLAYER_ID = 195001L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T03:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_signal_receipt")
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
    }

    @Autowired
    private QuestSignalProcessingService processingService;

    @Autowired
    private QuestSignalFingerprint signalFingerprint;

    @Autowired
    private QuestSignalProcessingAttempt processingAttempt;

    @Autowired
    private QuestSignalReceiptReplayRecovery replayRecovery;

    @MockitoSpyBean
    private QuestAcceptanceRepository acceptanceRepository;

    @MockitoBean
    private QuestProgressStore questProgressStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private QuestEventCommitProbe eventProbe;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id = ?",
                PLAYER_ID
        );
        eventProbe.reset();
        clearInvocations(questProgressStore);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Nested
    @DisplayName("Signal을 최초 적용하고 순차 Replay할 때")
    class ApplyThenReplay {

        @Test
        @DisplayName("Receipt와 Progress는 한 번만 남고 Replay는 Event와 Redis를 변경하지 않는다")
        void appliesExactlyOnceAfterRedisReset() {
            QuestSignal signal = signal(1, "source:collection:195001");

            QuestSignalProcessingResult applied =
                    processingService.process(signal);

            assertThat(applied.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.APPLIED);
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(acceptanceCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
            assertThat(acceptanceStatus())
                    .isEqualTo(QuestStatus.IN_PROGRESS.name());
            assertThat(eventProbe.types())
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS
                    );

            clearInvocations(questProgressStore);
            QuestSignalProcessingService restartedService =
                    new QuestSignalProcessingService(
                            signalFingerprint,
                            processingAttempt,
                            replayRecovery
                    );

            QuestSignalProcessingResult replayed =
                    restartedService.process(signal);

            assertThat(replayed.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.REPLAYED);
            assertThat(replayed.receiptId()).isEqualTo(applied.receiptId());
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(acceptanceCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
            assertThat(eventProbe.types())
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS
                    );
            verifyNoInteractions(questProgressStore);
        }
    }

    @Nested
    @DisplayName("동일 Signal 두 개가 동시에 도착할 때")
    class ProcessSameSignalConcurrently {

        @Test
        @DisplayName("한 요청만 APPLIED이고 다른 요청은 REPLAYED가 된다")
        void appliesOnlyOnce() throws Exception {
            QuestSignal signal = signal(1, "source:collection:concurrent");

            List<QuestSignalProcessingResult> results = runConcurrently(
                    () -> processingService.process(signal),
                    () -> processingService.process(signal)
            );

            assertThat(results)
                    .extracting(QuestSignalProcessingResult::outcome)
                    .containsExactlyInAnyOrder(
                            QuestSignalProcessingResult.Outcome.APPLIED,
                            QuestSignalProcessingResult.Outcome.REPLAYED
                    );
            assertThat(results)
                    .extracting(QuestSignalProcessingResult::receiptId)
                    .containsOnly(results.getFirst().receiptId());
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(acceptanceCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
            assertThat(eventProbe.types())
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS
                    );
            verify(questProgressStore, times(1)).set(
                    eq(QuestCode.COLLECTION_HUNTER_10),
                    eq(PLAYER_ID),
                    eq(1),
                    any(Duration.class)
            );
        }
    }

    @Nested
    @DisplayName("동일 Identity에 다른 Payload가 도착할 때")
    class RejectPayloadConflict {

        @Test
        @DisplayName("Replay로 처리하지 않고 안정된 Conflict Error를 반환한다")
        void rejectsConflict() {
            String correlation = "source:collection:conflict";
            processingService.process(signal(1, correlation));
            clearInvocations(questProgressStore);

            assertThatThrownBy(
                    () -> processingService.process(signal(2, correlation))
            ).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(
                                    QuestError
                                            .QUEST_SIGNAL_RECEIPT_PAYLOAD_CONFLICT
                            )
            );

            assertThat(receiptCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
            assertThat(eventProbe.types())
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS
                    );
            verifyNoInteractions(questProgressStore);
        }
    }

    @Nested
    @DisplayName("Receipt 저장 이후 Quest 처리에 실패할 때")
    class RollBackAndRetry {

        @Test
        @DisplayName("Receipt와 Acceptance를 rollback하고 같은 Signal을 다시 적용할 수 있다")
        void rollsBackThenApplies() {
            QuestSignal signal = signal(1, "source:collection:retry");
            doThrow(new RuntimeException("forced acceptance failure"))
                    .when(acceptanceRepository)
                    .save(any());

            assertThatThrownBy(() -> processingService.process(signal))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("forced acceptance failure");

            assertThat(receiptCount()).isZero();
            assertThat(acceptanceCount()).isZero();
            assertThat(eventProbe.types()).isEmpty();
            verifyNoInteractions(questProgressStore);

            reset(acceptanceRepository);
            QuestSignalProcessingResult retried =
                    processingService.process(signal);

            assertThat(retried.outcome())
                    .isEqualTo(QuestSignalProcessingResult.Outcome.APPLIED);
            assertThat(receiptCount()).isEqualTo(1);
            assertThat(acceptanceCount()).isEqualTo(1);
            assertThat(progressValue()).isEqualTo(1);
            assertThat(eventProbe.types())
                    .containsExactly(
                            QuestEventType.QUEST_ACCEPTED,
                            QuestEventType.QUEST_PROGRESS
                    );
        }
    }

    private QuestSignal signal(int delta, String correlationId) {
        return QuestSignal.addProgress(
                        QuestCode.COLLECTION_HUNTER_10,
                        PLAYER_ID,
                        delta
                )
                .occurredAt(OCCURRED_AT)
                .correlationId(correlationId)
                .attribute("category", "BOOK")
                .build();
    }

    private int receiptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_signal_receipts WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private int acceptanceCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quest_acceptances WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private int progressValue() {
        return jdbcTemplate.queryForObject(
                """
                SELECT progress_value
                FROM quest_acceptances
                WHERE player_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                Integer.class,
                PLAYER_ID
        );
    }

    private String acceptanceStatus() {
        return jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM quest_acceptances
                WHERE player_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                String.class,
                PLAYER_ID
        );
    }

    private <T> List<T> runConcurrently(
            CheckedSupplier<T> first,
            CheckedSupplier<T> second
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<T> firstFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return first.get();
        });
        Future<T> secondFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return second.get();
        });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(
                firstFuture.get(20, TimeUnit.SECONDS),
                secondFuture.get(20, TimeUnit.SECONDS)
        );
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @TestConfiguration
    static class EventProbeConfig {

        @Bean
        QuestEventCommitProbe questEventCommitProbe() {
            return new QuestEventCommitProbe();
        }
    }

    static class QuestEventCommitProbe {

        private final Queue<QuestEventType> eventTypes =
                new ConcurrentLinkedQueue<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onQuestEvent(QuestEvent event) {
            if (PLAYER_ID.equals(event.playerId())) {
                eventTypes.add(event.type());
            }
        }

        List<QuestEventType> types() {
            return List.copyOf(eventTypes);
        }

        void reset() {
            eventTypes.clear();
        }
    }
}
