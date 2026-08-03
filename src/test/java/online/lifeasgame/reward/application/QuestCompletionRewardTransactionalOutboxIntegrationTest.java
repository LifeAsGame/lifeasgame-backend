package online.lifeasgame.reward.application;

import online.lifeasgame.character.domain.error.PlayerError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.lifelog.domain.event.LifeLogRecorded;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.application.*;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import online.lifeasgame.quest.application.QuestManualCheckService;
import online.lifeasgame.quest.application.QuestService;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.QuestCode;
import online.lifeasgame.quest.domain.QuestStatus;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.event.QuestRewardReadyBridge;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.error.RewardError;
import org.junit.jupiter.api.*;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@Import(
        QuestCompletionRewardTransactionalOutboxIntegrationTest
                .MutableClockConfiguration.class
)
@DisplayName("Quest completion Reward/Outbox MySQL 통합")
class QuestCompletionRewardTransactionalOutboxIntegrationTest {

    private static final long PLAYER_ID = 219001L;
    private static final Instant ACCEPTED_AT =
            Instant.parse("2026-07-31T01:00:00Z");
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-31T02:00:00Z");

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_quest_completion_reward")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private QuestRewardReadyBridge bridge;

    @Autowired
    private QuestService questService;

    @Autowired
    private QuestManualCheckService manualCheckService;

    @Autowired
    private DomainEventPublisher eventPublisher;

    @Autowired
    private OutboxRelayService relayService;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxDispatchAttempt dispatchAttempt;

    @Autowired
    private OutboxCompletionService completionService;

    @Autowired
    private OutboxEventCodecRegistry codecRegistry;

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MutableClock clock;

    @MockitoSpyBean
    private RewardSettlementExpProcessService expProcessService;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        clock.set(ACCEPTED_AT);
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update(
                "DELETE FROM quest_signal_receipts WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update(
                "DELETE FROM quest_acceptances WHERE player_id = ?",
                PLAYER_ID
        );
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        insertPlayer();
        outboxProperties.setBatchSize(50);
        outboxProperties.setMaxAttempts(3);
        outboxProperties.setRetryDelayMs(0);
        outboxProperties.setInstanceId("quest-reward-integration");
        clearInvocations(expProcessService);
        transactionTemplate = new TransactionTemplate(transactionManager);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("RP_NONE은 Line 없이 COMPLETED이고 EXP를 지급하지 않는다")
    void completesNoRewardProfile() {
        bridge.onQuestEvent(rewardReady(219101L, "RP_NONE"));

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(lineCount()).isZero();
        assertThat(settlementStatus(219101L))
                .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        assertThat(playerExp()).isZero();
        assertThat(growthChangeCount()).isZero();
    }

    @Test
    @DisplayName("RP_EXP_TINY_10은 EXP 10을 exact-once 지급하고 Settlement를 완료한다")
    void processesExpTenExactlyOnceUnderConcurrentRedelivery()
            throws Exception {
        QuestEvent event = rewardReady(
                219102L,
                "RP_EXP_TINY_10"
        );
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = List.of(
                submit(event, ready, start),
                submit(event, ready, start)
        );

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
        }

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(lineCount()).isEqualTo(1);
        assertThat(lineStatus(219102L, "EXP"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(settlementStatus(219102L))
                .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        assertThat(playerExp()).isEqualTo(10L);
        assertThat(growthChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("RP_EXP_AND_ITEM_FIRST_STEP_20은 EXP만 성공시키고 ITEM과 Settlement를 PENDING으로 둔다")
    void processesExpAndLeavesItemPending() {
        bridge.onQuestEvent(rewardReady(
                219103L,
                "RP_EXP_AND_ITEM_FIRST_STEP_20"
        ));

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(lineCount()).isEqualTo(2);
        assertThat(lineStatus(219103L, "EXP"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(lineStatus(219103L, "ITEM"))
                .isEqualTo(RewardSettlementLineStatus.PENDING.name());
        assertThat(settlementStatus(219103L))
                .isEqualTo(RewardSettlementStatus.PENDING.name());
        assertThat(playerExp()).isEqualTo(20L);
        assertThat(growthChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("새 acceptance attempt는 별도 Settlement identity로 한 번 더 지급한다")
    void treatsRestartedAcceptanceAsNewSettlementIdentity() {
        bridge.onQuestEvent(rewardReady(
                219104L,
                "RP_EXP_TINY_10"
        ));
        bridge.onQuestEvent(rewardReady(
                219105L,
                "RP_EXP_TINY_10"
        ));

        assertThat(settlementCount()).isEqualTo(2);
        assertThat(playerExp()).isEqualTo(20L);
        assertThat(growthChangeCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Player가 없으면 FAILED/failureCode를 durable 저장하고 중복 Event에서 Processor를 재호출하지 않는다")
    void consumesOnlyDurableFailureAndSkipsFailedReplay() {
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        QuestEvent event = rewardReady(219106L, "RP_EXP_TINY_10");

        append(event);
        OutboxRelayResult first = relayService.relayBatch();
        append(event);
        OutboxRelayResult duplicate = relayService.relayBatch();

        assertThat(first.published()).isEqualTo(1);
        assertThat(duplicate.published()).isEqualTo(1);
        assertThat(lineStatus(219106L, "EXP"))
                .isEqualTo(RewardSettlementLineStatus.FAILED.name());
        assertThat(lineFailureCode(219106L, "EXP"))
                .isEqualTo(PlayerError.PLAYER_NOT_FOUND.code());
        assertThat(settlementStatus(219106L))
                .isEqualTo(RewardSettlementStatus.FAILED.name());
        assertThat(growthChangeCount()).isZero();
        verify(expProcessService, times(1))
                .process(anyLong(), anyLong());
    }

    @Test
    @DisplayName("동일 Settlement identity의 다른 profile은 Snapshot을 바꾸거나 추가 지급하지 않고 stable 409다")
    void rejectsProfileSnapshotConflict() {
        bridge.onQuestEvent(rewardReady(
                219107L,
                "RP_EXP_TINY_10"
        ));

        assertThatThrownBy(() -> bridge.onQuestEvent(
                rewardReady(219107L, "RP_NONE")
        )).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(
                                RewardError
                                        .REWARD_SETTLEMENT_SOURCE_PROFILE_CONFLICT
                        )
        );

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(settlementProfile(219107L))
                .isEqualTo("RP_EXP_TINY_10");
        assertThat(playerExp()).isEqualTo(10L);
        assertThat(growthChangeCount()).isEqualTo(1);
        verify(expProcessService, times(1))
                .process(anyLong(), anyLong());
    }

    @Test
    @DisplayName("서로 다른 profile의 동시 unique 경쟁은 winner Snapshot 하나와 loser conflict로 끝난다")
    void rejectsConcurrentProfileSnapshotLoser() throws Exception {
        long acceptanceId = 219108L;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = List.of(
                submit(
                        rewardReady(
                                acceptanceId,
                                "RP_EXP_TINY_10"
                        ),
                        ready,
                        start
                ),
                submit(
                        rewardReady(acceptanceId, "RP_NONE"),
                        ready,
                        start
                )
        );
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        int succeeded = 0;
        int conflicted = 0;
        for (Future<?> future : futures) {
            try {
                future.get(
                        Duration.ofSeconds(20).toMillis(),
                        TimeUnit.MILLISECONDS
                );
                succeeded++;
            } catch (ExecutionException exception) {
                assertThat(exception.getCause())
                        .isInstanceOfSatisfying(
                                DomainException.class,
                                cause -> assertThat(cause.getErrorCode())
                                        .isEqualTo(
                                                RewardError
                                                        .REWARD_SETTLEMENT_SOURCE_PROFILE_CONFLICT
                                        )
                        );
                conflicted++;
            }
        }

        assertThat(succeeded).isEqualTo(1);
        assertThat(conflicted).isEqualTo(1);
        assertThat(settlementCount()).isEqualTo(1);
        String profile = settlementProfile(acceptanceId);
        assertThat(profile).isIn("RP_EXP_TINY_10", "RP_NONE");
        assertThat(playerExp()).isEqualTo(
                profile.equals("RP_EXP_TINY_10") ? 10L : 0L
        );
        assertThat(growthChangeCount()).isEqualTo(
                profile.equals("RP_EXP_TINY_10") ? 1 : 0
        );
    }

    @Test
    @DisplayName("actual LifeLog AUTO closed loop는 Q_RECORD_FIRST_TRACE를 완료하고 EXP 10을 지급한다")
    void completesActualLifeLogAutoClosedLoop() {
        QuestResult.Acceptance accepted =
                accept(QuestCode.Q_RECORD_FIRST_TRACE);
        append(lifeLog(
                "219-auto-first",
                219201L,
                ACCEPTED_AT.plusSeconds(1)
        ));

        relayStages(3);

        assertThat(acceptanceStatus(accepted.id()))
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(settlementSourceIds())
                .containsExactly(accepted.id());
        assertThat(settlementProfile(accepted.id()))
                .isEqualTo("RP_EXP_TINY_10");
        assertThat(lineStatus(accepted.id(), "EXP"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(settlementStatus(accepted.id()))
                .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        assertThat(playerExp()).isEqualTo(10L);
        assertThat(growthChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("actual Manual Check USER_CONFIRM closed loop는 Q_GROWTH_ONE_FOCUS와 RP_NONE을 완료한다")
    void completesActualManualCheckClosedLoop() {
        QuestResult.Acceptance accepted =
                accept(QuestCode.Q_GROWTH_ONE_FOCUS);
        clock.set(ACCEPTED_AT.plusSeconds(60));

        QuestResult.Acceptance completed = manualCheckService.check(
                PLAYER_ID,
                new QuestCommand.ManualCheck(
                        QuestCode.Q_GROWTH_ONE_FOCUS.value()
                )
        );

        relayStages(2);

        assertThat(completed.id()).isEqualTo(accepted.id());
        assertThat(completed.status())
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(settlementProfile(accepted.id()))
                .isEqualTo("RP_NONE");
        assertThat(lineCount()).isZero();
        assertThat(settlementStatus(accepted.id()))
                .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        assertThat(playerExp()).isZero();
        assertThat(growthChangeCount()).isZero();
    }

    @Test
    @DisplayName("actual 세 LifeLog closed loop는 EXP 20만 성공시키고 ITEM을 PENDING으로 둔다")
    void completesActualExpAndItemClosedLoop() {
        QuestResult.Acceptance accepted =
                accept(QuestCode.Q_RECORD_THREE_TRACES);
        append(lifeLog(
                "219-three-a",
                219211L,
                ACCEPTED_AT.plusSeconds(1)
        ));
        append(lifeLog(
                "219-three-b",
                219212L,
                ACCEPTED_AT.plusSeconds(2)
        ));
        append(lifeLog(
                "219-three-c",
                219213L,
                ACCEPTED_AT.plusSeconds(3)
        ));

        relayStages(3);

        assertThat(acceptanceStatus(accepted.id()))
                .isEqualTo(QuestStatus.COMPLETED.name());
        assertThat(settlementProfile(accepted.id()))
                .isEqualTo("RP_EXP_AND_ITEM_FIRST_STEP_20");
        assertThat(lineStatus(accepted.id(), "EXP"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(lineStatus(accepted.id(), "ITEM"))
                .isEqualTo(RewardSettlementLineStatus.PENDING.name());
        assertThat(settlementStatus(accepted.id()))
                .isEqualTo(RewardSettlementStatus.PENDING.name());
        assertThat(playerExp()).isEqualTo(20L);
        assertThat(growthChangeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("actual AUTO QUEST_COMPLETED first-stage와 Reward Ready 중복 dispatch도 한 번만 지급한다")
    void redeliversActualFirstStageExactlyOnce() {
        QuestResult.Acceptance accepted =
                accept(QuestCode.Q_RECORD_FIRST_TRACE);
        append(lifeLog(
                "219-first-stage",
                219221L,
                ACCEPTED_AT.plusSeconds(1)
        ));
        assertThat(relayService.relayBatch().published()).isEqualTo(1);

        List<OutboxClaim> questClaims = claimService.claimBatch();
        assertThat(questClaims).hasSize(3);
        assertThat(questClaims.stream()
                .map(this::decodeQuestEvent)
                .filter(event ->
                        event.type() == QuestEventType.QUEST_COMPLETED
                )).hasSize(1);
        questClaims.forEach(claim -> {
            QuestEvent event = decodeQuestEvent(claim);
            dispatchAttempt.dispatch(claim);
            if (event.type() == QuestEventType.QUEST_COMPLETED) {
                dispatchAttempt.dispatch(claim);
            }
            completionService.complete(claim);
        });

        List<OutboxClaim> rewardClaims = claimService.claimBatch();
        assertThat(rewardClaims).hasSize(2);
        rewardClaims.forEach(claim -> {
            assertThat(decodeQuestEvent(claim).type())
                    .isEqualTo(QuestEventType.QUEST_REWARD_READY);
            dispatchAttempt.dispatch(claim);
            dispatchAttempt.dispatch(claim);
            completionService.complete(claim);
        });

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(settlementSourceIds())
                .containsExactly(accepted.id());
        assertThat(settlementProfile(accepted.id()))
                .isEqualTo("RP_EXP_TINY_10");
        assertThat(playerExp()).isEqualTo(10L);
        assertThat(growthChangeCount()).isEqualTo(1);
        assertThat(publishedOutboxCount()).isEqualTo(6);
    }

    private Future<?> submit(
            QuestEvent event,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            bridge.onQuestEvent(event);
            return null;
        });
    }

    private QuestResult.Acceptance accept(QuestCode questCode) {
        return questService.accept(
                PLAYER_ID,
                new QuestCommand.Accept(
                        questCode.value(),
                        null,
                        null
                )
        );
    }

    private void append(online.lifeasgame.core.event.DomainEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publish(event)
        );
    }

    private void relayStages(int count) {
        for (int stage = 0; stage < count; stage++) {
            OutboxRelayResult result = relayService.relayBatch();
            assertThat(result.failed()).isZero();
            assertThat(result.published()).isPositive();
        }
    }

    private LifeLogRecorded lifeLog(
            String eventId,
            Long lifeLogId,
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
                LifeLogSubtype.STUDY,
                LifeLogEntryMode.FULL,
                null,
                null,
                null,
                null
        );
    }

    private QuestEvent decodeQuestEvent(OutboxClaim claim) {
        return (QuestEvent) codecRegistry.decode(
                claim.eventType(),
                claim.payload()
        );
    }

    private QuestEvent rewardReady(long acceptanceId, String profileCode) {
        return new QuestEvent(
                QuestEventType.QUEST_REWARD_READY,
                PLAYER_ID,
                219L,
                "Q_FIRST_STEP",
                Map.of(
                        "acceptanceId", acceptanceId,
                        "rewardProfileCode", profileCode,
                        "questDefinitionVersion", 1,
                        "questSemanticCategory", "GROWTH",
                        "progressSource", "COUNT",
                        "repeatPolicy", "ONCE"
                ),
                COMPLETED_AT.plusSeconds(1),
                "quest:219:acceptance:%d:completed:reward".formatted(
                        acceptanceId
                )
        );
    }

    private void insertPlayer() {
        jdbcTemplate.update("""
                INSERT INTO player (
                    id, user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version, created_at, updated_at
                ) VALUES (
                    ?, ?, 'Quest Reward Tester', 'male', 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, PLAYER_ID + 100000L);
    }

    private int settlementCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_settlements",
                Integer.class
        );
    }

    private int lineCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_settlement_lines",
                Integer.class
        );
    }

    private String settlementStatus(long acceptanceId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT status
                FROM reward_settlements
                WHERE player_id = ?
                  AND source_type = 'QUEST_COMPLETION'
                  AND source_id = ?
                """,
                String.class,
                PLAYER_ID,
                acceptanceId
        );
    }

    private String lineStatus(long acceptanceId, String rewardType) {
        return jdbcTemplate.queryForObject(
                """
                SELECT line.status
                FROM reward_settlement_lines line
                JOIN reward_settlements settlement
                  ON settlement.id = line.reward_settlement_id
                WHERE settlement.player_id = ?
                  AND settlement.source_type = 'QUEST_COMPLETION'
                  AND settlement.source_id = ?
                  AND line.reward_type = ?
                """,
                String.class,
                PLAYER_ID,
                acceptanceId,
                rewardType
        );
    }

    private String lineFailureCode(
            long acceptanceId,
            String rewardType
    ) {
        return jdbcTemplate.queryForObject(
                """
                SELECT line.failure_code
                FROM reward_settlement_lines line
                JOIN reward_settlements settlement
                  ON settlement.id = line.reward_settlement_id
                WHERE settlement.player_id = ?
                  AND settlement.source_type = 'QUEST_COMPLETION'
                  AND settlement.source_id = ?
                  AND line.reward_type = ?
                """,
                String.class,
                PLAYER_ID,
                acceptanceId,
                rewardType
        );
    }

    private String settlementProfile(long acceptanceId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT reward_profile_code
                FROM reward_settlements
                WHERE player_id = ?
                  AND source_type = 'QUEST_COMPLETION'
                  AND source_id = ?
                """,
                String.class,
                PLAYER_ID,
                acceptanceId
        );
    }

    private List<Long> settlementSourceIds() {
        return jdbcTemplate.queryForList(
                """
                SELECT source_id
                FROM reward_settlements
                WHERE player_id = ?
                  AND source_type = 'QUEST_COMPLETION'
                ORDER BY source_id
                """,
                Long.class,
                PLAYER_ID
        );
    }

    private String acceptanceStatus(long acceptanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM quest_acceptances WHERE id = ?",
                String.class,
                acceptanceId
        );
    }

    private long playerExp() {
        return jdbcTemplate.queryForObject(
                "SELECT exp FROM player WHERE id = ?",
                Long.class,
                PLAYER_ID
        );
    }

    private int growthChangeCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player_growth_changes",
                Integer.class
        );
    }

    private int publishedOutboxCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = 'PUBLISHED'",
                Integer.class
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MutableClockConfiguration {

        @Bean
        @Primary
        MutableClock questRewardClock() {
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
