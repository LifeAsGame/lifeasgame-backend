package online.lifeasgame.lifelog.quick.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.application.command.CollectionCommand;
import online.lifeasgame.lifelog.application.command.ExerciseCommand;
import online.lifeasgame.lifelog.application.command.MediaLogCommand;
import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;
import online.lifeasgame.lifelog.quick.domain.error.QuickRecordError;
import online.lifeasgame.lifelog.quick.domain.repository.QuickRecordRequestReceiptRepository;
import online.lifeasgame.platform.outbox.application.OutboxRelayScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(QuickRecordServiceIntegrationTest.QuickRecordTestConfiguration.class)
@DisplayName("QuickRecord MySQL subtype dispatch와 멱등성")
class QuickRecordServiceIntegrationTest {

    private static final String RECORDED_ALIAS = "lifelog.recorded.v1";
    private static final String COLLECTION_ALIAS =
            "lifelog.collection-logged.v1";
    private static final String EXERCISE_ALIAS =
            "lifelog.exercise-logged.v1";
    private static final String MEDIA_ADVANCED_ALIAS =
            "lifelog.media-advanced.v1";
    private static final Long PLAYER_ID = 201001L;
    private static final Long OTHER_PLAYER_ID = 201002L;
    private static final Instant RECORDED_AT =
            Instant.parse("2026-07-24T14:00:00.123456Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quick_record")
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
    private QuickRecordService quickRecordService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FailingDomainEventPublisher eventPublisher;

    @Autowired
    private FailingQuickRecordReceiptRepository receiptRepository;

    @Autowired
    private FailingLifeLogRecordRepository lifeLogRecordRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM quick_record_request_receipts");
        jdbcTemplate.update("DELETE FROM collection_log_tags");
        jdbcTemplate.update("DELETE FROM media_log_tags");
        jdbcTemplate.update("DELETE FROM life_log_records");
        jdbcTemplate.update("DELETE FROM collection_logs");
        jdbcTemplate.update("DELETE FROM exercise_logs");
        jdbcTemplate.update("DELETE FROM media_logs");
        jdbcTemplate.update("DELETE FROM outbox_events");
        eventPublisher.reset();
        receiptRepository.reset();
        lifeLogRecordRepository.reset();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
                .isTrue();
    }

    @Test
    @DisplayName("Collection Quick Record는 기존 Source와 두 기존 Event만 저장한다")
    void dispatchesCollectionCreate() throws Exception {
        QuickRecordResult.Recorded result = quickRecordService.record(
                PLAYER_ID,
                "collection-first",
                collectionCommand("Private collection title")
        );

        assertFirstResult(result, LifeLogType.COLLECTION);
        assertThat(count("collection_logs")).isEqualTo(1);
        assertThat(count("collection_log_tags")).isEqualTo(2);
        assertThat(count("exercise_logs")).isZero();
        assertThat(count("media_logs")).isZero();
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount(COLLECTION_ALIAS)).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(2);
        assertRecordedPayload(result);
        assertThat(storedReceipt()).isEqualTo(
                new StoredReceipt(
                        LifeLogType.COLLECTION.name(),
                        result.sourceId(),
                        RECORDED_AT
                )
        );
        assertThat(tableExists("quick_lifelog_entries")).isFalse();
    }

    @Test
    @DisplayName("Exercise Quick Record는 기존 Source와 두 기존 Event만 저장한다")
    void dispatchesExerciseCreate() throws Exception {
        QuickRecordResult.Recorded result = quickRecordService.record(
                PLAYER_ID,
                "exercise-first",
                exerciseCommand(30)
        );

        assertFirstResult(result, LifeLogType.EXERCISE);
        assertThat(count("collection_logs")).isZero();
        assertThat(count("exercise_logs")).isEqualTo(1);
        assertThat(count("media_logs")).isZero();
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount(EXERCISE_ALIAS)).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(2);
        assertRecordedPayload(result);
    }

    @Test
    @DisplayName("Media Quick Record는 기존 create 계약의 LifeLogRecorded만 저장한다")
    void dispatchesMediaCreate() throws Exception {
        QuickRecordResult.Recorded result = quickRecordService.record(
                PLAYER_ID,
                "media-first",
                mediaCommand("Private media title")
        );

        assertFirstResult(result, LifeLogType.MEDIA);
        assertThat(count("collection_logs")).isZero();
        assertThat(count("exercise_logs")).isZero();
        assertThat(count("media_logs")).isEqualTo(1);
        assertThat(count("media_log_tags")).isEqualTo(1);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount(MEDIA_ADVANCED_ALIAS)).isZero();
        assertThat(outboxCount()).isEqualTo(1);
        assertRecordedPayload(result);
    }

    @Test
    @DisplayName("동일 key와 payload 순차 replay는 subtype create와 Event를 반복하지 않는다")
    void replaysSequentially() {
        QuickRecordCommand.Create command =
                collectionCommand("Replay collection");
        QuickRecordResult.Recorded first = quickRecordService.record(
                PLAYER_ID,
                " replay-key ",
                command
        );
        QuickRecordResult.Recorded replay = quickRecordService.record(
                PLAYER_ID,
                "replay-key",
                command
        );

        assertThat(first.replay()).isFalse();
        assertThat(replay.replay()).isTrue();
        assertSameSnapshot(first, replay);
        assertThat(count("collection_logs")).isEqualTo(1);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount(COLLECTION_ALIAS)).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("동일 key의 다른 payload와 다른 subtype은 기존 Source를 보존하고 409다")
    void conflictsSequentially() {
        QuickRecordResult.Recorded winner = quickRecordService.record(
                PLAYER_ID,
                "conflict-key",
                collectionCommand("Winner collection")
        );
        StoredReceipt before = storedReceipt();

        assertConflict(() -> quickRecordService.record(
                PLAYER_ID,
                "conflict-key",
                collectionCommand("Changed collection")
        ));
        assertConflict(() -> quickRecordService.record(
                PLAYER_ID,
                "conflict-key",
                mediaCommand("Other subtype")
        ));

        assertThat(storedReceipt()).isEqualTo(before);
        assertThat(storedReceipt().sourceId())
                .isEqualTo(winner.sourceId());
        assertThat(storedCollectionTitle())
                .isEqualTo("Winner collection");
        assertThat(count("collection_logs")).isEqualTo(1);
        assertThat(count("media_logs")).isZero();
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 Player는 같은 key로 독립 Source를 생성한다")
    void scopesKeyByPlayer() {
        QuickRecordResult.Recorded first = quickRecordService.record(
                PLAYER_ID,
                "shared-key",
                mediaCommand("First player")
        );
        QuickRecordResult.Recorded second = quickRecordService.record(
                OTHER_PLAYER_ID,
                "shared-key",
                mediaCommand("Second player")
        );

        assertThat(first.sourceId()).isNotEqualTo(second.sourceId());
        assertThat(count("media_logs")).isEqualTo(2);
        assertThat(receiptCount()).isEqualTo(2);
        assertThat(count("life_log_records")).isEqualTo(2);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 key와 payload 동시 요청은 Source 1건과 replay 1건을 만든다")
    void serializesSamePayloadConcurrently() throws Exception {
        QuickRecordCommand.Create command =
                mediaCommand("Concurrent same media");

        List<QuickRecordResult.Recorded> results = runConcurrently(
                () -> quickRecordService.record(
                        PLAYER_ID,
                        "concurrent-same",
                        command
                ),
                () -> quickRecordService.record(
                        PLAYER_ID,
                        "concurrent-same",
                        command
                )
        );

        assertThat(results)
                .extracting(QuickRecordResult.Recorded::replay)
                .containsExactlyInAnyOrder(false, true);
        assertSameSnapshot(results.getFirst(), results.getLast());
        assertThat(count("media_logs")).isEqualTo(1);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 key와 다른 payload 동시 요청은 승자 Source만 남긴다")
    void conflictsDifferentPayloadConcurrently() throws Exception {
        List<ConcurrentOutcome> outcomes = runConcurrently(
                () -> capture(() -> quickRecordService.record(
                        PLAYER_ID,
                        "concurrent-conflict",
                        collectionCommand("Concurrent first")
                )),
                () -> capture(() -> quickRecordService.record(
                        PLAYER_ID,
                        "concurrent-conflict",
                        collectionCommand("Concurrent second")
                ))
        );

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.result() != null)
                .singleElement()
                .satisfies(outcome ->
                        assertThat(outcome.result().replay()).isFalse()
                );
        assertThat(outcomes)
                .filteredOn(outcome -> outcome.error() != null)
                .singleElement()
                .satisfies(outcome ->
                        assertThat(outcome.error().getErrorCode())
                                .isEqualTo(
                                        QuickRecordError
                                                .IDEMPOTENCY_KEY_PAYLOAD_CONFLICT
                                )
                );
        assertThat(storedCollectionTitle())
                .isIn("Concurrent first", "Concurrent second");
        assertThat(count("collection_logs")).isEqualTo(1);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(outboxCount(COLLECTION_ALIAS)).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("기존 subtype validation 실패는 예약 Receipt까지 rollback한다")
    void rollsBackSubtypeValidationFailure() {
        assertThatThrownBy(() -> quickRecordService.record(
                PLAYER_ID,
                "validation-failure",
                exerciseCommand(0)
        )).isInstanceOf(IllegalStateException.class);

        assertNoQuickRecordMutation();
    }

    @Test
    @DisplayName("Receipt result 저장 실패는 subtype과 기존 Event를 함께 rollback한다")
    void rollsBackReceiptFailure() {
        receiptRepository.failNextCompletion();

        assertThatThrownBy(() -> quickRecordService.record(
                PLAYER_ID,
                "receipt-failure",
                collectionCommand("Receipt rollback")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced receipt failure");

        assertNoQuickRecordMutation();
    }

    @Test
    @DisplayName("Outbox append 실패는 subtype과 Receipt를 함께 rollback한다")
    void rollsBackOutboxFailure() {
        eventPublisher.failNext();

        assertThatThrownBy(() -> quickRecordService.record(
                PLAYER_ID,
                "outbox-failure",
                mediaCommand("Outbox rollback")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced outbox failure");

        assertNoQuickRecordMutation();
    }

    @Test
    @DisplayName("canonical header 저장 실패는 Source와 Receipt/Outbox를 함께 rollback한다")
    void rollsBackHeaderFailure() {
        lifeLogRecordRepository.failNext();

        assertThatThrownBy(() -> quickRecordService.record(
                PLAYER_ID,
                "header-failure",
                mediaCommand("Header rollback")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("forced header failure");

        assertNoQuickRecordMutation();
    }

    @Test
    @DisplayName("Content-ready Quick metadata는 QUICK weekly Snapshot으로 저장한다")
    void recordsContentReadyWeeklyMetadata() throws Exception {
        QuickRecordCommand.Create command = new QuickRecordCommand.Create(
                "MEDIA",
                "REFLECTION",
                "WEEKLY_LOOKBACK",
                null,
                null,
                new MediaLogCommand.Create(
                        "MOVIE",
                        "Weekly reflection",
                        null,
                        0,
                        1,
                        "PLANNED",
                        Set.of()
                )
        );

        QuickRecordResult.Recorded result = quickRecordService.record(
                PLAYER_ID,
                "weekly-reflection",
                command
        );
        QuickRecordResult.Recorded replay = quickRecordService.record(
                PLAYER_ID,
                "weekly-reflection",
                command
        );
        QuickRecordCommand.Create changedMetadata =
                new QuickRecordCommand.Create(
                        "MEDIA",
                        "STUDY",
                        null,
                        null,
                        null,
                        command.media()
                );
        assertConflict(() -> quickRecordService.record(
                PLAYER_ID,
                "weekly-reflection",
                changedMetadata
        ));
        JsonNode json = recordedPayload();

        assertThat(replay.replay()).isTrue();
        assertSameSnapshot(result, replay);
        assertThat(count("life_log_records")).isEqualTo(1);
        assertThat(outboxCount(RECORDED_ALIAS)).isEqualTo(1);
        assertThat(json.get("lifeLogId").asLong())
                .isEqualTo(headerId(result));
        assertThat(json.get("sourceDefinitionVersion").asInt())
                .isEqualTo(1);
        assertThat(json.get("subtype").asText())
                .isEqualTo("REFLECTION");
        assertThat(json.get("entryMode").asText())
                .isEqualTo(LifeLogEntryMode.QUICK.name());
        assertThat(json.get("reflectionScope").asText())
                .isEqualTo("WEEKLY_LOOKBACK");
        assertThat(json.get("periodKey").asText())
                .isEqualTo("2026-W30");
        assertThat(json.has("lifeLogType")).isFalse();
        assertThat(json.has("sourceType")).isFalse();
    }

    @Test
    @DisplayName("통합 테스트 profile은 Outbox Scheduler를 비활성화한다")
    void disablesScheduler() {
        assertThat(applicationContext.getBeansOfType(
                OutboxRelayScheduler.class
        )).isEmpty();
    }

    private QuickRecordCommand.Create collectionCommand(String title) {
        return new QuickRecordCommand.Create(
                "COLLECTION",
                new CollectionCommand.Create(
                        "BOOK",
                        title,
                        "Private original",
                        1,
                        "Private condition",
                        "Private source",
                        Set.of(" Daily ", "BOOK")
                ),
                null,
                null
        );
    }

    private QuickRecordCommand.Create exerciseCommand(
            int durationMinutes
    ) {
        return new QuickRecordCommand.Create(
                "EXERCISE",
                null,
                new ExerciseCommand.Create(
                        "RUNNING",
                        durationMinutes,
                        5.0,
                        250,
                        LocalDate.of(2026, 7, 24),
                        "Private memo"
                ),
                null
        );
    }

    private QuickRecordCommand.Create mediaCommand(String title) {
        return new QuickRecordCommand.Create(
                "MEDIA",
                null,
                null,
                new MediaLogCommand.Create(
                        "MOVIE",
                        title,
                        "Private original",
                        0,
                        1,
                        "PLANNED",
                        Set.of("private-tag")
                )
        );
    }

    private void assertFirstResult(
            QuickRecordResult.Recorded result,
            LifeLogType type
    ) {
        assertThat(result.sourceType()).isEqualTo(type);
        assertThat(result.sourceId()).isPositive();
        assertThat(result.recordedAt()).isEqualTo(RECORDED_AT);
        assertThat(result.replay()).isFalse();
    }

    private void assertSameSnapshot(
            QuickRecordResult.Recorded first,
            QuickRecordResult.Recorded second
    ) {
        assertThat(second.sourceType()).isEqualTo(first.sourceType());
        assertThat(second.sourceId()).isEqualTo(first.sourceId());
        assertThat(second.recordedAt()).isEqualTo(first.recordedAt());
    }

    private void assertConflict(Callable<?> call) {
        assertThatThrownBy(call::call)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            QuickRecordError
                                                    .IDEMPOTENCY_KEY_PAYLOAD_CONFLICT
                                    );
                            assertThat(exception.detail()).isNull();
                        }
                );
    }

    private void assertNoQuickRecordMutation() {
        assertThat(count("collection_logs")).isZero();
        assertThat(count("exercise_logs")).isZero();
        assertThat(count("media_logs")).isZero();
        assertThat(count("life_log_records")).isZero();
        assertThat(receiptCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    private ConcurrentOutcome capture(
            Callable<QuickRecordResult.Recorded> call
    ) {
        try {
            return new ConcurrentOutcome(call.call(), null);
        } catch (DomainException exception) {
            return new ConcurrentOutcome(null, exception);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private <T> List<T> runConcurrently(
            Callable<T> first,
            Callable<T> second
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<T> firstFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return first.call();
        });
        Future<T> secondFuture = executor.submit(() -> {
            ready.countDown();
            start.await();
            return second.call();
        });
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        return List.of(
                firstFuture.get(20, TimeUnit.SECONDS),
                secondFuture.get(20, TimeUnit.SECONDS)
        );
    }

    private void assertRecordedPayload(
            QuickRecordResult.Recorded result
    ) throws Exception {
        JsonNode json = recordedPayload();
        assertThat(json.get("eventType").asText())
                .isEqualTo("LifeLogRecorded");
        assertThat(json.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(json.get("playerId").asLong()).isEqualTo(PLAYER_ID);
        assertThat(json.get("lifeLogId").asLong())
                .isEqualTo(headerId(result));
        assertThat(json.get("sourceDefinitionVersion").asInt())
                .isEqualTo(1);
        assertThat(json.get("subtype").isNull()).isTrue();
        assertThat(json.get("entryMode").asText())
                .isEqualTo(LifeLogEntryMode.QUICK.name());
        assertThat(json.has("lifeLogType")).isFalse();
        assertThat(json.has("sourceType")).isFalse();
        assertThat(json.get("occurredAt").asText())
                .isEqualTo(RECORDED_AT.toString());
    }

    private JsonNode recordedPayload() throws Exception {
        String payload = jdbcTemplate.queryForObject(
                """
                SELECT payload
                FROM outbox_events
                WHERE event_type = ?
                """,
                String.class,
                RECORDED_ALIAS
        );
        return objectMapper.readTree(payload);
    }

    private Long headerId(QuickRecordResult.Recorded result) {
        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM life_log_records
                WHERE source_type = ?
                  AND source_id = ?
                """,
                Long.class,
                result.sourceType().name(),
                result.sourceId()
        );
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table,
                Integer.class
        );
    }

    private int receiptCount() {
        return count("quick_record_request_receipts");
    }

    private int outboxCount() {
        return count("outbox_events");
    }

    private int outboxCount(String alias) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM outbox_events
                WHERE event_type = ?
                """,
                Integer.class,
                alias
        );
    }

    private String storedCollectionTitle() {
        return jdbcTemplate.queryForObject(
                "SELECT title_value FROM collection_logs",
                String.class
        );
    }

    private StoredReceipt storedReceipt() {
        return jdbcTemplate.queryForObject(
                """
                SELECT source_type, source_id, recorded_at
                FROM quick_record_request_receipts
                """,
                (resultSet, rowNum) -> new StoredReceipt(
                        resultSet.getString("source_type"),
                        resultSet.getLong("source_id"),
                        resultSet.getObject(
                                        "recorded_at",
                                        LocalDateTime.class
                                )
                                .toInstant(ZoneOffset.UTC)
                )
        );
    }

    private boolean tableExists(String table) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) > 0
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Boolean.class,
                table
        );
    }

    private record StoredReceipt(
            String sourceType,
            Long sourceId,
            Instant recordedAt
    ) {
    }

    private record ConcurrentOutcome(
            QuickRecordResult.Recorded result,
            DomainException error
    ) {
    }

    @TestConfiguration
    static class QuickRecordTestConfiguration {

        @Bean
        @Primary
        Clock quickRecordTestClock() {
            return Clock.fixed(RECORDED_AT, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        FailingDomainEventPublisher quickRecordEventPublisher(
                @Qualifier("transactionalOutboxDomainEventPublisher")
                DomainEventPublisher delegate
        ) {
            return new FailingDomainEventPublisher(delegate);
        }

        @Bean
        @Primary
        FailingQuickRecordReceiptRepository
        quickRecordReceiptRepository(
                @Qualifier("quickRecordRequestReceiptRepositoryAdapter")
                QuickRecordRequestReceiptRepository delegate
        ) {
            return new FailingQuickRecordReceiptRepository(delegate);
        }

        @Bean
        @Primary
        FailingLifeLogRecordRepository lifeLogRecordRepository(
                @Qualifier("lifeLogRecordRepositoryAdapter")
                LifeLogRecordRepository delegate
        ) {
            return new FailingLifeLogRecordRepository(delegate);
        }
    }

    static class FailingDomainEventPublisher
            implements DomainEventPublisher {

        private final DomainEventPublisher delegate;
        private final AtomicBoolean failNext = new AtomicBoolean();

        FailingDomainEventPublisher(DomainEventPublisher delegate) {
            this.delegate = delegate;
        }

        @Override
        public void publish(DomainEvent event) {
            if (failNext.getAndSet(false)) {
                throw new IllegalStateException(
                        "forced outbox failure"
                );
            }
            delegate.publish(event);
        }

        void failNext() {
            failNext.set(true);
        }

        void reset() {
            failNext.set(false);
        }
    }

    static class FailingQuickRecordReceiptRepository
            implements QuickRecordRequestReceiptRepository {

        private final QuickRecordRequestReceiptRepository delegate;
        private final AtomicBoolean failNextCompletion =
                new AtomicBoolean();

        FailingQuickRecordReceiptRepository(
                QuickRecordRequestReceiptRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public void reserve(
                Long playerId,
                String idempotencyKey,
                String requestHash,
                Instant reservedAt
        ) {
            delegate.reserve(
                    playerId,
                    idempotencyKey,
                    requestHash,
                    reservedAt
            );
        }

        @Override
        public Optional<QuickRecordRequestReceipt>
        findByIdentityForUpdate(
                Long playerId,
                String idempotencyKey
        ) {
            return delegate.findByIdentityForUpdate(
                    playerId,
                    idempotencyKey
            );
        }

        @Override
        public QuickRecordRequestReceipt saveAndFlush(
                QuickRecordRequestReceipt receipt
        ) {
            if (failNextCompletion.getAndSet(false)) {
                throw new IllegalStateException(
                        "forced receipt failure"
                );
            }
            return delegate.saveAndFlush(receipt);
        }

        void failNextCompletion() {
            failNextCompletion.set(true);
        }

        void reset() {
            failNextCompletion.set(false);
        }
    }

    static class FailingLifeLogRecordRepository
            implements LifeLogRecordRepository {

        private final LifeLogRecordRepository delegate;
        private final AtomicBoolean failNext = new AtomicBoolean();

        FailingLifeLogRecordRepository(
                LifeLogRecordRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public LifeLogRecord saveAndFlush(LifeLogRecord record) {
            if (failNext.getAndSet(false)) {
                throw new IllegalStateException(
                        "forced header failure"
                );
            }
            return delegate.saveAndFlush(record);
        }

        @Override
        public Optional<LifeLogRecord> findBySource(
                LifeLogSourceType sourceType,
                Long sourceId
        ) {
            return delegate.findBySource(sourceType, sourceId);
        }

        void failNext() {
            failNext.set(true);
        }

        void reset() {
            failNext.set(false);
        }
    }
}
