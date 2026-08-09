package online.lifeasgame.reward.application;

import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.reward.application.event.QuestRewardReadyBridge;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("QuestCompletion ITEM Reward closed loop MySQL 통합")
class QuestCompletionItemRewardIntegrationTest {

    private static final long PLAYER_ID = 226201L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_quest_item_reward")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private QuestRewardReadyBridge bridge;

    @MockitoSpyBean
    private InventoryRewardDeliveryApi inventoryDeliveryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM player_growth_changes");
        jdbcTemplate.update("DELETE FROM inventory_reward_deliveries");
        jdbcTemplate.update("DELETE FROM inventory_entries");
        jdbcTemplate.update("DELETE FROM mailbox_entries");
        jdbcTemplate.update("DELETE FROM player_inventory");
        jdbcTemplate.update("DELETE FROM player_mailbox");
        jdbcTemplate.update("DELETE FROM reward_settlement_lines");
        jdbcTemplate.update("DELETE FROM reward_settlements");
        jdbcTemplate.update("DELETE FROM player WHERE id = ?", PLAYER_ID);
        insertPlayer();
        clearInvocations(inventoryDeliveryApi);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("동시·순차·restart redelivery에도 EXP/ITEM/receipt를 각각 한 번만 반영한다")
    void completesMixedProfileExactlyOnceAcrossReplays() throws Exception {
        QuestRewardReadyFact event = rewardReady();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = List.of(
                submit(event, ready, start),
                submit(event, ready, start)
        );
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(
                    Duration.ofSeconds(30).toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }

        clearInvocations(inventoryDeliveryApi);
        bridge.onQuestRewardReady(event);

        assertThat(settlementCount()).isEqualTo(1);
        assertThat(lineCount()).isEqualTo(2);
        assertThat(lineStatus("EXP"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(lineStatus("ITEM"))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(settlementStatus())
                .isEqualTo(RewardSettlementStatus.COMPLETED.name());
        assertThat(playerExp()).isEqualTo(20L);
        assertThat(growthChangeCount()).isEqualTo(1);
        assertThat(mailboxQuantity()).isEqualTo(1L);
        assertThat(receiptCount()).isEqualTo(1);
        verify(inventoryDeliveryApi, never()).deliverReward(
                anyLong(), anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong()
        );
        verify(inventoryDeliveryApi).findRewardDelivery(anyLong());
    }

    private Future<?> submit(
            QuestRewardReadyFact event,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            bridge.onQuestRewardReady(event);
            return null;
        });
    }

    private QuestRewardReadyFact rewardReady() {
        return new QuestRewardReadyFact(
                QuestRewardReadyFact.EVENT_VERSION,
                PLAYER_ID,
                226301L,
                "RP_EXP_AND_ITEM_FIRST_STEP_20",
                2262L,
                "Q_RECORD_THREE_TRACES",
                1,
                Instant.parse("2026-08-03T12:00:00Z"),
                "quest:2262:acceptance:226301:completed:reward"
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
                    ?, ?, 'ITEM Reward Tester', 'male', 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
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

    private String lineStatus(String rewardType) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                FROM reward_settlement_lines
                WHERE reward_type = ?
                """, String.class, rewardType);
    }

    private String settlementStatus() {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_settlements",
                String.class
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

    private long mailboxQuantity() {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM mailbox_entries",
                Long.class
        );
    }

    private int receiptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_reward_deliveries",
                Integer.class
        );
    }
}
