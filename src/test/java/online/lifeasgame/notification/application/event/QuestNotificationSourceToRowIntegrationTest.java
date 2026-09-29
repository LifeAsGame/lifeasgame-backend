package online.lifeasgame.notification.application.event;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.notification.domain.NotificationType;
import online.lifeasgame.notification.domain.PlayerNotification;
import online.lifeasgame.platform.outbox.application.OutboxClaim;
import online.lifeasgame.platform.outbox.application.OutboxClaimService;
import online.lifeasgame.platform.outbox.application.OutboxCompletionService;
import online.lifeasgame.platform.outbox.application.OutboxDispatchAttempt;
import online.lifeasgame.platform.outbox.application.OutboxRelayResult;
import online.lifeasgame.platform.outbox.application.OutboxRelayService;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.infra.QuestRewardReadyPublicationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(QuestNotificationSourceToRowIntegrationTest.ClockConfiguration.class)
@DisplayName("Quest Notification source-to-row MySQL 통합")
class QuestNotificationSourceToRowIntegrationTest {

    private static final long PLAYER_ID = 325001L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-08-01T01:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-08-01T02:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_notification")
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
        registry.add("app.outbox.batch-size", () -> 50);
        registry.add("app.outbox.max-attempts", () -> 3);
        registry.add("app.outbox.retry-delay-ms", () -> 0);
        registry.add("app.outbox.instance-id", () -> "quest-notification-proof");
    }

    @Autowired
    private QuestService questService;

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxDispatchAttempt dispatchAttempt;

    @MockitoSpyBean
    private OutboxCompletionService completionService;

    @MockitoSpyBean
    private QuestRewardReadyPublicationStore publicationStore;

    @Autowired
    private OutboxEventCodecRegistry codecRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MutableClock clock;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.set(ACCEPTED_AT);
        jdbcTemplate.update("DELETE FROM player_notifications");
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM quest_reward_ready_publications");
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id IN (?, ?)",
                PLAYER_ID, PLAYER_ID + 1
        );
        jdbcTemplate.update(
                "DELETE FROM player_quest_routes WHERE player_id IN (?, ?)",
                PLAYER_ID, PLAYER_ID + 1
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id IN (?, ?)",
                PLAYER_ID, PLAYER_ID + 1
        );
        jdbcTemplate.update("DELETE FROM player WHERE id IN (?, ?)", PLAYER_ID, PLAYER_ID + 1);
        insertPlayer(PLAYER_ID);
        selectRecordRoute();
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("실제 Quest 완료 source의 재전달도 완료 알림 한 건과 미진행 Route를 남긴다")
    void storesQuestCompletedOnceWithoutAdvancingRoute() {
        completeFirstTraceQuest();
        RouteState routeBeforeDispatch = routeState();

        List<OutboxClaim> questClaims = claimService.claimBatch();
        OutboxClaim completedClaim = completedClaim(questClaims);
        QuestEvent completedEvent = decodeQuestEvent(completedClaim);
        String originalTitle = (String) completedEvent.attributes().get("questTitle");
        assertThat(originalTitle).isNotBlank();
        jdbcTemplate.update("UPDATE quests SET title_id = '변경된 Quest 제목' WHERE id = ?", completedEvent.questId());
        questClaims.forEach(claim -> {
            dispatchAttempt.dispatch(claim);
            if (claim.equals(completedClaim)) {
                dispatchAttempt.dispatch(claim);
            }
            completionService.complete(claim);
        });

        assertNotification(
                NotificationType.QUEST_COMPLETED,
                completedClaim.eventId(),
                completedEvent.occurredAt(),
                "Quest를 완료했어요",
                completedEvent.attributes().get("questTitle") + " 완료 사실이 기록되었습니다."
        );
        assertThat(routeState()).isEqualTo(routeBeforeDispatch);
        jdbcTemplate.update("UPDATE quests SET title_id = ? WHERE id = ?", originalTitle, completedEvent.questId());
    }

    @Test
    @DisplayName("완료 부모를 ack 전에 재전달해도 자식과 보상 준비 알림 및 EXP는 한 번만 남는다")
    void publishesRewardReadyOnceAfterParentReplay() {
        var accepted = completeFirstTraceQuest();
        List<OutboxClaim> parents = claimService.claimBatch();
        OutboxClaim completed = completedClaim(parents);
        dispatchAttempt.dispatch(completed);
        // Child transaction has committed, but the parent has not been acknowledged.
        dispatchAttempt.dispatch(completed);
        assertThat(rewardReadyCount()).isEqualTo(1);
        parents.forEach(completionService::complete);

        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_notifications WHERE type = 'QUEST_REWARD_READY'", Long.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_settlements WHERE source_id = ?", Long.class, accepted.id()))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT exp FROM player WHERE id = ?", Long.class, PLAYER_ID)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_growth_changes WHERE player_id = ?", Long.class, PLAYER_ID))
                .isEqualTo(1);
    }

    @ParameterizedTest(name = "lock timeout = {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("동시 부모 재전달과 잠금 timeout 후 재시도 모두 자식 한 건으로 수렴한다")
    void retriesConcurrentParentDelivery(boolean forceTimeout) throws Exception {
        completeFirstTraceQuest();
        OutboxClaim parent = completedClaim(claimService.claimBatch());
        CountDownLatch claimed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch concurrentStarted = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        QuestRewardReadyPublicationStore targetStore = AopTestUtils.getUltimateTargetObject(publicationStore);
        doAnswer(invocation -> {
            jdbcTemplate.execute("SET SESSION innodb_lock_wait_timeout = " + (forceTimeout ? 1 : 50));
            if (claimed.getCount() == 0) concurrentStarted.countDown();
            try {
                boolean owner = (boolean) invocation.callRealMethod();
                if (owner && first.compareAndSet(true, false)) {
                    claimed.countDown();
                    assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                }
                return owner;
            } finally {
                jdbcTemplate.execute("SET SESSION innodb_lock_wait_timeout = 50");
            }
        }).when(targetStore).claim(anyString());

        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstDelivery = executor.submit(() -> dispatchAttempt.dispatch(parent));
            assertThat(claimed.await(10, TimeUnit.SECONDS)).isTrue();
            var concurrentDelivery = executor.submit(() -> dispatchAttempt.dispatch(parent));
            assertThat(concurrentStarted.await(10, TimeUnit.SECONDS)).isTrue();
            if (forceTimeout) {
                assertThatThrownBy(() -> concurrentDelivery.get(10, TimeUnit.SECONDS))
                        .hasStackTraceContaining("Lock wait timeout exceeded");
            }
            release.countDown();
            firstDelivery.get(10, TimeUnit.SECONDS);
            if (!forceTimeout) concurrentDelivery.get(10, TimeUnit.SECONDS);
            dispatchAttempt.dispatch(parent);
            assertThat(rewardReadyCount()).isEqualTo(1);
            assertThat(publicationCount()).isEqualTo(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("자식 outbox insert 실패는 claim도 롤백하고 부모 재시도에서 발행한다")
    void rollsBackClaimWhenChildInsertFails() {
        completeFirstTraceQuest();
        jdbcTemplate.execute("ALTER TABLE outbox_events ADD CONSTRAINT reject_reward_ready CHECK (event_type <> 'quest.reward-ready.v1')");
        try {
            assertThat(relayService.relayBatch().failed()).isEqualTo(1);
            assertThat(rewardReadyCount()).isZero();
            assertThat(publicationCount()).isZero();
        } finally {
            jdbcTemplate.execute("ALTER TABLE outbox_events DROP CHECK reject_reward_ready");
        }
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(rewardReadyCount()).isEqualTo(1);
        assertThat(publicationCount()).isEqualTo(1);
        assertThat(relayService.relayBatch().failed()).isZero();
        assertRewardEffects(PLAYER_ID, 1);
    }

    @Test
    @DisplayName("자식 commit 이후 부모 ack가 실패해도 lease 회복과 재전달은 기존 자식을 유지한다")
    void recoversAfterParentAcknowledgementFailure() {
        completeFirstTraceQuest();
        AtomicBoolean failOnce = new AtomicBoolean(true);
        doAnswer(invocation -> {
            OutboxClaim claim = invocation.getArgument(0);
            if (decodeQuestEvent(claim).type() == QuestEventType.QUEST_COMPLETED
                    && failOnce.compareAndSet(true, false)) {
                throw new IllegalStateException("parent acknowledgement failed");
            }
            return invocation.callRealMethod();
        }).when(completionService).complete(any(OutboxClaim.class));

        assertThatThrownBy(() -> relayService.relayBatch())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("parent acknowledgement failed");
        assertThat(rewardReadyCount()).isEqualTo(1);
        assertThat(publicationCount()).isEqualTo(1);
        clock.set(COMPLETED_AT.plusSeconds(31));
        // The child can be dispatched in the same recovery batch as the parent.
        doAnswer(invocation -> invocation.callRealMethod())
                .when(completionService).complete(any(OutboxClaim.class));
        assertThat(relayService.relayBatch().recovered()).isPositive();
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(rewardReadyCount()).isEqualTo(1);
        assertThat(publicationCount()).isEqualTo(1);
        assertRewardEffects(PLAYER_ID, 1);
    }

    @Test
    @DisplayName("완료 알림 저장이 실패해도 자식 발행과 정산은 유지하고 부모 재시도는 중복하지 않는다")
    void retainsPublicationWhenCompletionNotificationFails() {
        completeFirstTraceQuest();
        jdbcTemplate.execute("ALTER TABLE player_notifications ADD CONSTRAINT reject_completion_notification CHECK (type <> 'QUEST_COMPLETED')");
        try {
            assertThat(relayService.relayBatch().failed()).isEqualTo(1);
            assertThat(rewardReadyCount()).isEqualTo(1);
            assertThat(publicationCount()).isEqualTo(1);
            assertThat(relayService.relayBatch().failed()).isEqualTo(1);
            assertRewardEffects(PLAYER_ID, 1);
        } finally {
            jdbcTemplate.execute("ALTER TABLE player_notifications DROP CHECK reject_completion_notification");
        }
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(rewardReadyCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_notifications WHERE type = 'QUEST_COMPLETED'", Long.class))
                .isEqualTo(1);
        assertRewardEffects(PLAYER_ID, 1);
    }

    @Test
    @DisplayName("서로 다른 실제 완료 acceptance는 각각 자식과 보상을 받는다")
    void publishesForIndependentCompletions() {
        var first = completeFirstTraceQuest();
        relayQuestEventsOnce();
        assertThat(relayService.relayBatch().failed()).isZero();
        insertPlayer(PLAYER_ID + 1);
        clock.set(ACCEPTED_AT);
        var second = completeFirstTraceQuest(PLAYER_ID + 1);
        relayQuestEventsOnce();
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(rewardReadyCount()).isEqualTo(2);
        assertThat(publicationCount()).isEqualTo(2);
        assertRewardEffects(PLAYER_ID, 1);
        assertRewardEffects(PLAYER_ID + 1, 1);
    }

    @Test
    @DisplayName("legacy reward-ready 부모도 한 번만 변환하며 원래 시간과 correlation을 유지한다")
    void deduplicatesLegacyConversion() {
        completeFirstTraceQuest();
        List<OutboxClaim> parents = claimService.claimBatch();
        QuestEvent completed = decodeQuestEvent(completedClaim(parents));
        parents.forEach(completionService::complete);
        QuestEvent legacy = new QuestEvent(QuestEventType.QUEST_REWARD_READY,
                completed.playerId(), completed.questId(), completed.questCode(),
                completed.attributes(), completed.occurredAt(), completed.correlationId());
        append(legacy);
        OutboxClaim parent = claimService.claimBatch().getFirst();
        dispatchAttempt.dispatch(parent);
        dispatchAttempt.dispatch(parent);
        completionService.complete(parent);
        assertThat(rewardReadyCount()).isEqualTo(1);
        assertThat(publicationCount()).isEqualTo(1);
        OutboxClaim child = claimService.claimBatch().getFirst();
        QuestRewardReadyFact fact = (QuestRewardReadyFact) codecRegistry.decode(child.eventType(), child.payload());
        assertThat(fact.occurredAt()).isEqualTo(legacy.occurredAt());
        assertThat(fact.correlationId()).isEqualTo(legacy.correlationId());
        dispatchAttempt.dispatch(child);
        completionService.complete(child);
        assertRewardEffects(PLAYER_ID, 1);
    }

    private long publicationCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quest_reward_ready_publications", Long.class);
    }

    private void assertRewardEffects(long playerId, long count) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_notifications WHERE player_id = ? AND type = 'QUEST_REWARD_READY'",
                Long.class, playerId)).isEqualTo(count);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_settlements WHERE player_id = ? AND status = 'COMPLETED'",
                Long.class, playerId)).isEqualTo(count);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_growth_changes WHERE player_id = ?", Long.class, playerId))
                .isEqualTo(count);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT exp FROM player WHERE id = ?", Long.class, playerId)).isEqualTo(count * 10);
    }

    private long rewardReadyCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = 'quest.reward-ready.v1'", Long.class);
    }

    @Test
    @DisplayName("실제 typed reward-ready Fact의 재전달도 보상 준비 알림 한 건만 남긴다")
    void storesQuestRewardReadyOnce() {
        completeFirstTraceQuest();
        relayQuestEventsOnce();

        List<OutboxClaim> rewardClaims = claimService.claimBatch();
        assertThat(rewardClaims).hasSize(1);
        OutboxClaim rewardClaim = rewardClaims.getFirst();
        assertThat(rewardClaim.eventType()).isEqualTo("quest.reward-ready.v1");
        QuestRewardReadyFact rewardFact =
                (QuestRewardReadyFact) codecRegistry.decode(
                        rewardClaim.eventType(),
                        rewardClaim.payload()
                );

        dispatchAttempt.dispatch(rewardClaim);
        dispatchAttempt.dispatch(rewardClaim);
        completionService.complete(rewardClaim);

        assertNotification(
                NotificationType.QUEST_REWARD_READY,
                rewardClaim.eventId(),
                rewardFact.occurredAt(),
                "Quest 보상이 준비됐어요",
                rewardFact.questTitle() + "의 확인 가능한 보상이 준비되었습니다. Mailbox 또는 결과 화면에서 상태를 확인해 주세요."
        );
    }

    @Test
    @DisplayName("알림 저장 실패에도 완료·보상 정산은 유지되고 outbox 재시도가 같은 event row로 수렴한다")
    void retriesNotificationWithoutUndoingSource() {
        var accepted = completeFirstTraceQuest();
        relayQuestEventsOnce();
        String eventId = jdbcTemplate.queryForObject(
                "SELECT event_id FROM outbox_events WHERE event_type = 'quest.reward-ready.v1'", String.class);
        jdbcTemplate.execute("ALTER TABLE player_notifications ADD CONSTRAINT force_notification_failure CHECK (type <> 'QUEST_REWARD_READY')");
        try {
            var failed = relayService.relayBatch();
            assertThat(failed.failed()).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT status FROM reward_settlements WHERE source_id = ?", String.class, accepted.id()))
                    .isEqualTo("COMPLETED");
            assertThat(jdbcTemplate.queryForObject("SELECT completed_at FROM quest_acceptances WHERE id = ?", Object.class, accepted.id())).isNotNull();
            assertThat(jdbcTemplate.queryForObject("SELECT exp FROM player WHERE id = ?", Long.class, PLAYER_ID)).isEqualTo(10);
            var failure = jdbcTemplate.queryForMap("SELECT attempt_count, last_error FROM outbox_events WHERE event_id = ?", eventId);
            assertThat(((Number) failure.get("attempt_count")).intValue()).isEqualTo(1);
            assertThat(failure.get("last_error").toString()).contains("Dispatch failed");
        } finally {
            jdbcTemplate.execute("ALTER TABLE player_notifications DROP CHECK force_notification_failure");
        }
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM player_notifications WHERE source_event_id = ?", Long.class, eventId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT exp FROM player WHERE id = ?", Long.class, PLAYER_ID)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM outbox_events WHERE event_id = ?", String.class, eventId)).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("제목 없는 legacy fact는 보상을 되돌리지 않고 관찰 가능한 재시도·FAILED로 남으며 copy를 추측하지 않는다")
    void observesMissingLegacySnapshotFailure() {
        var accepted = completeFirstTraceQuest();
        relayQuestEventsOnce();
        jdbcTemplate.update("UPDATE outbox_events SET payload = JSON_REMOVE(payload, '$.questTitle') WHERE event_type = 'quest.reward-ready.v1'");
        for (int i = 0; i < 3; i++) assertThat(relayService.relayBatch().failed()).isEqualTo(1);
        var failed = jdbcTemplate.queryForMap("SELECT status, attempt_count, last_error FROM outbox_events WHERE event_type = 'quest.reward-ready.v1'");
        assertThat(failed.get("status")).isEqualTo("FAILED");
        assertThat(((Number) failed.get("attempt_count")).intValue()).isEqualTo(3);
        assertThat(failed.get("last_error").toString()).contains("DomainException");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM player_notifications WHERE type = 'QUEST_REWARD_READY'", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reward_settlements WHERE source_id = ?", String.class, accepted.id()))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT exp FROM player WHERE id = ?", Long.class, PLAYER_ID)).isEqualTo(10);
    }

    @Test
    @DisplayName("현재 Quest 정의가 없는 과거 완료 이벤트도 저장된 제목 snapshot으로만 렌더링한다")
    void rendersWithoutCurrentDefinition() {
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quests WHERE id = 99999999", Long.class)).isZero();
        append(QuestEvent.builder(QuestEventType.QUEST_COMPLETED).playerId(PLAYER_ID).questId(99999999L)
                .questCode("DELETED_DEFINITION").attribute("questTitle", "삭제 전 Quest")
                .occurredAt(COMPLETED_AT).correlationId("deleted-definition-snapshot").build());
        assertThat(relayService.relayBatch().failed()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT body FROM player_notifications", String.class))
                .isEqualTo("삭제 전 Quest 완료 사실이 기록되었습니다.");
    }

    private QuestResult.Acceptance completeFirstTraceQuest() {
        return completeFirstTraceQuest(PLAYER_ID);
    }

    private QuestResult.Acceptance completeFirstTraceQuest(long playerId) {
        QuestResult.Acceptance accepted = questService.accept(
                playerId,
                new QuestCommand.Accept(
                        QuestCode.Q_RECORD_FIRST_TRACE.value(),
                        null,
                        null
                )
        );
        append(new LifeLogRecorded(
                "life-log-recorded-" + playerId,
                LifeLogRecorded.EVENT_TYPE,
                LifeLogRecorded.EVENT_VERSION,
                ACCEPTED_AT.plusSeconds(1),
                playerId,
                playerId + 100L,
                1,
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null,
                null,
                null
        ));
        clock.set(COMPLETED_AT);

        OutboxRelayResult sourceRelay = relayService.relayBatch();
        assertThat(sourceRelay.failed()).isZero();
        assertThat(sourceRelay.published()).isEqualTo(1);
        return accepted;
    }

    private void relayQuestEventsOnce() {
        OutboxRelayResult questRelay = relayService.relayBatch();
        assertThat(questRelay.failed()).isZero();
        assertThat(questRelay.published()).isEqualTo(3);
    }

    private void append(online.lifeasgame.core.event.DomainEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publish(event)
        );
    }

    private OutboxClaim completedClaim(List<OutboxClaim> claims) {
        List<OutboxClaim> completed = claims.stream()
                .filter(claim -> claim.eventType().equals("quest.event.v1"))
                .filter(claim -> decodeQuestEvent(claim).type()
                        == QuestEventType.QUEST_COMPLETED)
                .toList();
        assertThat(completed).hasSize(1);
        return completed.getFirst();
    }

    private QuestEvent decodeQuestEvent(OutboxClaim claim) {
        return (QuestEvent) codecRegistry.decode(
                claim.eventType(),
                claim.payload()
        );
    }

    private void assertNotification(
            NotificationType type,
            String sourceEventId,
            Instant occurredAt,
            String title,
            String body
    ) {
        entityManager.clear();
        List<PlayerNotification> notifications = entityManager.createQuery(
                        """
                        SELECT notification
                        FROM PlayerNotification notification
                        WHERE notification.playerId = :playerId
                          AND notification.type = :type
                        """,
                        PlayerNotification.class
                )
                .setParameter("playerId", PLAYER_ID)
                .setParameter("type", type.name())
                .getResultList();

        assertThat(notifications).hasSize(1);
        PlayerNotification notification = notifications.getFirst();
        assertThat(notification.getPlayerId()).isEqualTo(PLAYER_ID);
        assertThat(notification.getSourceEventId()).isEqualTo(sourceEventId);
        assertThat(notification.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(notification.getTitle()).isEqualTo(title);
        assertThat(notification.getBody()).isEqualTo(body);
        String copyKey = type == NotificationType.QUEST_COMPLETED
                ? "notification.ntf_quest_completed" : "notification.ntf_quest_reward_ready";
        assertThat(notification.getTitleCopyId()).isEqualTo(copyKey + ".title");
        assertThat(notification.getBodyCopyId()).isEqualTo(copyKey + ".body");
        assertThat(notification.getTitleCopyVersion()).isEqualTo(1);
        assertThat(notification.getBodyCopyVersion()).isEqualTo(1);
        assertThat(notification.getCopyLocale()).isEqualTo("ko-KR");
    }

    private void insertPlayer(long playerId) {
        jdbcTemplate.update("""
                INSERT INTO player (
                    id, user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version, created_at, updated_at
                ) VALUES (
                    ?, ?, 'Quest Notification Tester', 'male', 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, playerId, playerId + 100000L);
    }

    private void selectRecordRoute() {
        jdbcTemplate.update("""
                INSERT INTO player_quest_routes (
                    player_id, route_id, current_step_id, status,
                    selected_at, completed_at, version, created_at, updated_at
                )
                SELECT ?, route.id, step.id, 'IN_PROGRESS',
                       CURRENT_TIMESTAMP(6), NULL, 0,
                       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                FROM quest_routes route
                JOIN quest_route_steps step ON step.route_id = route.id
                WHERE route.code = 'ROUTE_RECORD_START'
                  AND step.step_code = 'RS_RECORD_01_LEAVE_TRACE'
                """, PLAYER_ID);
    }

    private RouteState routeState() {
        return jdbcTemplate.queryForObject("""
                SELECT player_route.current_step_id,
                       player_route.status,
                       player_route.version,
                       player_route.completed_at
                FROM player_quest_routes player_route
                JOIN quest_routes route ON route.id = player_route.route_id
                WHERE player_route.player_id = ?
                  AND route.code = 'ROUTE_RECORD_START'
                """, (resultSet, rowNumber) -> new RouteState(
                resultSet.getLong("current_step_id"),
                resultSet.getString("status"),
                resultSet.getLong("version"),
                resultSet.getObject("completed_at")
        ), PLAYER_ID);
    }

    private record RouteState(
            long currentStepId,
            String status,
            long version,
            Object completedAt
    ) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableClock questNotificationClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {

        private final AtomicReference<Instant> current;
        private final ZoneId zone;

        MutableClock() {
            this(new AtomicReference<>(ACCEPTED_AT), ZoneOffset.UTC);
        }

        private MutableClock(
                AtomicReference<Instant> current,
                ZoneId zone
        ) {
            this.current = current;
            this.zone = Objects.requireNonNull(zone, "zone");
        }

        void set(Instant instant) {
            current.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
