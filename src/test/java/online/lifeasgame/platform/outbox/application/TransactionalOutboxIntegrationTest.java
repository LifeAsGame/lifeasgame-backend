package online.lifeasgame.platform.outbox.application;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.domain.event.CollectionLogged;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.domain.OutboxStatus;
import online.lifeasgame.social.domain.ChatChannelType;
import online.lifeasgame.social.domain.event.ChatChannelDeactivated;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(TransactionalOutboxIntegrationTest.ListenerConfiguration.class)
@DisplayName("Transactional Outbox MySQL 처리")
class TransactionalOutboxIntegrationTest {

    private static final Long QUEST_PLAYER_ID = 197197L;
    private static final Instant OCCURRED_AT =
            Instant.parse("2026-07-24T08:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_outbox")
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
    private DomainEventPublisher domainEventPublisher;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxDispatchAttempt dispatchAttempt;

    @Autowired
    private OutboxCompletionService completionService;

    @Autowired
    private OutboxLeaseRecoveryService leaseRecoveryService;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private FailingAfterCommitProbe eventProbe;

    @Autowired
    private ApplicationContext applicationContext;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id = ?",
                QUEST_PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id = ?",
                QUEST_PLAYER_ID
        );
        properties.setBatchSize(50);
        properties.setMaxAttempts(10);
        properties.setRetryDelayMs(0);
        properties.setLeaseDurationMs(30_000);
        properties.setInstanceId("outbox-integration-a");
        eventProbe.reset();
        transactionTemplate = new TransactionTemplate(transactionManager);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Nested
    @DisplayName("Test Profile로 Context를 기동할 때")
    class SchedulerSafety {

        @Test
        @DisplayName("Scheduler를 생성하지 않아 명시적 Relay와 경쟁하지 않는다")
        void disablesScheduler() {
            assertThat(
                    applicationContext.getBeansOfType(
                            OutboxRelayScheduler.class
                    )
            ).isEmpty();
        }
    }

    @Nested
    @DisplayName("DomainEvent를 Outbox에 append할 때")
    class AppendEvent {

        @Test
        @DisplayName("비즈니스 Transaction commit과 함께 PENDING 한 행을 저장한다")
        void appendsOnCommit() {
            append(chatEvent(1L));

            assertThat(outboxCount()).isEqualTo(1);
            assertThat(statusOfOnlyEvent()).isEqualTo(OutboxStatus.PENDING);
        }

        @Test
        @DisplayName("비즈니스 Transaction rollback이면 Outbox도 남지 않는다")
        void rollsBackTogether() {
            transactionTemplate.executeWithoutResult(status -> {
                domainEventPublisher.publish(chatEvent(2L));
                status.setRollbackOnly();
            });

            assertThat(outboxCount()).isZero();
        }

        @Test
        @DisplayName("publishAll은 Event마다 한 행을 저장한다")
        void appendsEveryEvent() {
            transactionTemplate.executeWithoutResult(status ->
                    domainEventPublisher.publishAll(
                            List.of(chatEvent(3L), chatEvent(4L))
                    )
            );

            assertThat(outboxCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Transaction 밖 publish는 거부한다")
        void rejectsOutsideTransaction() {
            assertThatThrownBy(() ->
                    domainEventPublisher.publish(chatEvent(5L))
            ).isInstanceOf(org.springframework.transaction
                    .IllegalTransactionStateException.class);

            assertThat(outboxCount()).isZero();
        }
    }

    @Nested
    @DisplayName("PENDING Event를 Relay할 때")
    class RelayPendingEvent {

        @Test
        @DisplayName("AFTER_COMMIT Listener 성공 후 PUBLISHED로 완료한다")
        void publishesAfterListenerCommit() {
            append(chatEvent(11L));

            OutboxRelayResult result = relayService.relayBatch();

            assertThat(result.claimed()).isEqualTo(1);
            assertThat(result.published()).isEqualTo(1);
            assertThat(result.failed()).isZero();
            assertThat(statusOfOnlyEvent())
                    .isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(eventProbe.deliveries()).isEqualTo(1);
        }

        @Test
        @DisplayName("Listener 실패는 PENDING retry로 돌아가고 다음 성공에 완료한다")
        void retriesListenerFailure() {
            properties.setMaxAttempts(3);
            eventProbe.failNext(1);
            append(chatEvent(12L));

            OutboxRelayResult first = relayService.relayBatch();
            assertThat(first.failed()).isEqualTo(1);
            assertThat(statusOfOnlyEvent()).isEqualTo(OutboxStatus.PENDING);
            assertThat(attemptCountOfOnlyEvent()).isEqualTo(1);

            OutboxRelayResult second = relayService.relayBatch();

            assertThat(second.published()).isEqualTo(1);
            assertThat(statusOfOnlyEvent())
                    .isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(eventProbe.deliveries()).isEqualTo(2);
        }

        @Test
        @DisplayName("최대 실패 횟수에 도달하면 FAILED로 종료한다")
        void failsPermanentlyAtMaxAttempts() {
            properties.setMaxAttempts(2);
            eventProbe.failNext(2);
            append(chatEvent(13L));

            relayService.relayBatch();
            relayService.relayBatch();

            assertThat(statusOfOnlyEvent()).isEqualTo(OutboxStatus.FAILED);
            assertThat(attemptCountOfOnlyEvent()).isEqualTo(2);
            assertThat(lastErrorOfOnlyEvent())
                    .isEqualTo("Dispatch failed (IllegalStateException)");
        }

        @Test
        @DisplayName("PUBLISHED Event는 다시 claim하지 않는다")
        void doesNotRepublishPublishedEvent() {
            append(chatEvent(14L));
            relayService.relayBatch();

            OutboxRelayResult replay = relayService.relayBatch();

            assertThat(replay.claimed()).isZero();
            assertThat(eventProbe.deliveries()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("다중 Relay가 동시에 claim할 때")
    class ClaimConcurrently {

        @Test
        @DisplayName("SKIP LOCKED로 각 Row를 한 Relay만 claim한다")
        void claimsEachRowOnce() throws Exception {
            properties.setBatchSize(5);
            for (long id = 20; id < 30; id++) {
                append(chatEvent(id));
            }
            CountDownLatch start = new CountDownLatch(1);

            List<List<OutboxClaim>> results = invokeConcurrently(
                    () -> {
                        start.await();
                        return claimService.claimBatch();
                    },
                    () -> {
                        start.await();
                        return claimService.claimBatch();
                    },
                    start
            );

            List<Long> ids = results.stream()
                    .flatMap(List::stream)
                    .map(OutboxClaim::id)
                    .toList();
            assertThat(ids).isNotEmpty();
            assertThat(new HashSet<>(ids)).hasSize(ids.size());
            assertThat(countByStatus(OutboxStatus.PROCESSING))
                    .isEqualTo(ids.size());

            List<Long> remaining = claimService.claimBatch().stream()
                    .map(OutboxClaim::id)
                    .toList();
            List<Long> allClaimed = new CopyOnWriteArrayList<>(ids);
            allClaimed.addAll(remaining);

            assertThat(allClaimed).hasSize(10);
            assertThat(new HashSet<>(allClaimed)).hasSize(10);
            assertThat(countByStatus(OutboxStatus.PROCESSING))
                    .isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("PROCESSING lease가 만료될 때")
    class RecoverStaleLease {

        @Test
        @DisplayName("stale Row를 PENDING으로 복구한다")
        void recoversStaleProcessingEvent() {
            append(chatEvent(31L));
            claimService.claimBatch();
            jdbcTemplate.update("""
                    UPDATE outbox_events
                    SET locked_at = CURRENT_TIMESTAMP(6) - INTERVAL 60 SECOND
                    WHERE status = 'PROCESSING'
                    """);

            int recovered = leaseRecoveryService.recoverStale();

            assertThat(recovered).isEqualTo(1);
            assertThat(statusOfOnlyEvent()).isEqualTo(OutboxStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Listener 성공 후 완료 표시 전에 응답을 잃을 때")
    class CrashWindow {

        @Test
        @DisplayName("lease 복구 뒤 at-least-once로 다시 전달한다")
        void redeliversAfterLeaseRecovery() {
            append(chatEvent(41L));
            OutboxClaim claim = claimService.claimBatch().getFirst();

            dispatchAttempt.dispatch(claim);
            assertThat(eventProbe.deliveries()).isEqualTo(1);
            assertThat(statusOfOnlyEvent())
                    .isEqualTo(OutboxStatus.PROCESSING);

            jdbcTemplate.update("""
                    UPDATE outbox_events
                    SET locked_at = CURRENT_TIMESTAMP(6) - INTERVAL 60 SECOND
                    WHERE id = ?
                    """, claim.id());

            OutboxRelayResult recovered = relayService.relayBatch();

            assertThat(recovered.recovered()).isEqualTo(1);
            assertThat(recovered.published()).isEqualTo(1);
            assertThat(eventProbe.deliveries()).isEqualTo(2);
            assertThat(statusOfOnlyEvent())
                    .isEqualTo(OutboxStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("Quest Source Event를 중복 전달할 때")
    class DuplicateQuestSource {

        @Test
        @DisplayName("QuestSignalReceipt가 중복 Progress를 차단한다")
        void preventsDuplicateQuestProgress() {
            append(new CollectionLogged(
                    QUEST_PLAYER_ID,
                    197001L,
                    "BOOK",
                    1,
                    OCCURRED_AT
            ));
            OutboxClaim source = claimService.claimBatch().getFirst();

            dispatchAttempt.dispatch(source);
            assertThat(questReceiptCount()).isEqualTo(1);
            assertThat(questProgress()).isEqualTo(1);

            dispatchAttempt.dispatch(source);

            assertThat(questReceiptCount()).isEqualTo(1);
            assertThat(questProgress()).isEqualTo(1);
        }
    }

    private void append(DomainEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                domainEventPublisher.publish(event)
        );
    }

    private ChatChannelDeactivated chatEvent(Long channelId) {
        return new ChatChannelDeactivated(
                channelId,
                ChatChannelType.GUILD,
                OCCURRED_AT.plusSeconds(channelId),
                "inactive"
        );
    }

    private int outboxCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Integer.class
        );
    }

    private int countByStatus(OutboxStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = ?",
                Integer.class,
                status.name()
        );
    }

    private OutboxStatus statusOfOnlyEvent() {
        return OutboxStatus.valueOf(
                jdbcTemplate.queryForObject(
                        "SELECT status FROM outbox_events",
                        String.class
                )
        );
    }

    private int attemptCountOfOnlyEvent() {
        return jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM outbox_events",
                Integer.class
        );
    }

    private String lastErrorOfOnlyEvent() {
        return jdbcTemplate.queryForObject(
                "SELECT last_error FROM outbox_events",
                String.class
        );
    }

    private int questReceiptCount() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM quest_signal_receipts
                WHERE player_id = ?
                """,
                Integer.class,
                QUEST_PLAYER_ID
        );
    }

    private int questProgress() {
        return jdbcTemplate.queryForObject(
                """
                SELECT progress_value
                FROM quest_acceptances
                WHERE player_id = ?
                """,
                Integer.class,
                QUEST_PLAYER_ID
        );
    }

    private <T> List<T> invokeConcurrently(
            Callable<T> first,
            Callable<T> second,
            CountDownLatch start
    ) throws Exception {
        Queue<Future<T>> futures = new ConcurrentLinkedQueue<>();
        futures.add(executor.submit(first));
        futures.add(executor.submit(second));
        start.countDown();

        List<T> results = new CopyOnWriteArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get(15, TimeUnit.SECONDS));
        }
        return results;
    }

    @TestConfiguration
    static class ListenerConfiguration {

        @Bean
        FailingAfterCommitProbe failingAfterCommitProbe() {
            return new FailingAfterCommitProbe();
        }
    }

    static class FailingAfterCommitProbe {

        private final AtomicInteger deliveries = new AtomicInteger();
        private final AtomicInteger failuresRemaining = new AtomicInteger();

        @EventListener
        public void on(ChatChannelDeactivated event) {
            deliveries.incrementAndGet();
            if (failuresRemaining.getAndUpdate(value ->
                    Math.max(0, value - 1)
            ) > 0) {
                throw new IllegalStateException("listener failed");
            }
        }

        void failNext(int count) {
            failuresRemaining.set(count);
        }

        int deliveries() {
            return deliveries.get();
        }

        void reset() {
            deliveries.set(0);
            failuresRemaining.set(0);
        }
    }
}
