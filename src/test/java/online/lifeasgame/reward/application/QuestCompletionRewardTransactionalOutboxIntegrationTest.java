package online.lifeasgame.reward.application;

import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.platform.outbox.OutboxProperties;
import online.lifeasgame.platform.outbox.application.*;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.reward.application.event.QuestRewardReadyBridge;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Quest completion Reward/Outbox MySQL 통합")
class QuestCompletionRewardTransactionalOutboxIntegrationTest {

    private static final long PLAYER_ID = 219001L;
    private static final Instant COMPLETED_AT =
            Instant.parse("2026-07-30T03:00:00Z");

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
    private DomainEventPublisher eventPublisher;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private OutboxDispatchAttempt dispatchAttempt;

    @Autowired
    private OutboxCompletionService completionService;

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        insertPlayer();
        outboxProperties.setBatchSize(50);
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
    @DisplayName("LifeLog/Manual 완료의 두 단계 Outbox를 재전달해도 acceptance별 EXP는 한 번만 지급한다")
    void redeliversTwoStageOutboxExactlyOnce() {
        appendCompleted(
                219106L,
                "RP_EXP_TINY_10",
                Map.of("lifeLogId", 901L)
        );
        appendCompleted(
                219107L,
                "RP_EXP_TINY_10",
                Map.of("manualCheck", true)
        );

        List<OutboxClaim> completionClaims = claimService.claimBatch();
        assertThat(completionClaims).hasSize(2);
        completionClaims.forEach(claim -> {
            dispatchAttempt.dispatch(claim);
            completionService.complete(claim);
        });

        List<OutboxClaim> rewardClaims = claimService.claimBatch();
        assertThat(rewardClaims).hasSize(2);
        rewardClaims.forEach(claim -> {
            dispatchAttempt.dispatch(claim);
            dispatchAttempt.dispatch(claim);
            completionService.complete(claim);
        });

        assertThat(settlementCount()).isEqualTo(2);
        assertThat(playerExp()).isEqualTo(20L);
        assertThat(growthChangeCount()).isEqualTo(2);
        assertThat(publishedOutboxCount()).isEqualTo(4);
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

    private void appendCompleted(
            long acceptanceId,
            String profileCode,
            Map<String, Object> sourceContext
    ) {
        Map<String, Object> attributes =
                new java.util.LinkedHashMap<>(sourceContext);
        attributes.put("acceptanceId", acceptanceId);
        attributes.put("rewardProfileCode", profileCode);
        attributes.put("questSemanticCategory", "GROWTH");
        attributes.put("progressSource", "COUNT");
        attributes.put("repeatPolicy", "ONCE");
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publish(new QuestEvent(
                        QuestEventType.QUEST_COMPLETED,
                        PLAYER_ID,
                        219L,
                        "Q_FIRST_STEP",
                        attributes,
                        COMPLETED_AT,
                        "quest:219:acceptance:%d:completed".formatted(
                                acceptanceId
                        )
                ))
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
}
