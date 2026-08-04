package online.lifeasgame.reward.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.reward.application.result.RewardSettlementItemProcessResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.RewardSettlementLineStatus;
import online.lifeasgame.reward.domain.RewardSettlementSourceType;
import online.lifeasgame.reward.domain.RewardSettlementStatus;
import online.lifeasgame.reward.domain.RewardType;
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("RewardSettlement ITEM 처리 MySQL 통합")
class RewardSettlementItemProcessIntegrationTest {

    private static final long PLAYER_ID = 226001L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_item_reward_process")
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
    private RewardSettlementCreateService createService;

    @Autowired
    private RewardSettlementItemProcessService processService;

    @Autowired
    private RewardSettlementExpProcessService expProcessService;

    @MockitoSpyBean
    private InventoryRewardDeliveryApi inventoryDeliveryApi;

    @MockitoSpyBean
    private RewardSettlementWriter settlementWriter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
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
        clearInvocations(inventoryDeliveryApi, settlementWriter);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("동시 같은 ITEM Line은 한 번 지급하고 success/replay로 끝난다")
    void deliversSameLineExactlyOnceConcurrently() throws Exception {
        RewardSettlement settlement = createMixedSettlement(226101L);
        RewardSettlementLine itemLine = itemLine(settlement);

        List<RewardSettlementItemProcessResult> results = runConcurrently(
                () -> processService.process(settlement.getId(), itemLine.getId()),
                () -> processService.process(settlement.getId(), itemLine.getId())
        );

        assertThat(results)
                .extracting(RewardSettlementItemProcessResult::replayed)
                .containsExactlyInAnyOrder(false, true);
        assertThat(mailboxQuantity()).isEqualTo(1L);
        assertThat(receiptCount(itemLine.getId())).isEqualTo(1);
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
        assertThat(settlementStatus(settlement.getId()))
                .isEqualTo(RewardSettlementStatus.PENDING.name());
    }

    @Test
    @DisplayName("PENDING Line의 matching receipt는 중복 지급 없이 성공 복구한다")
    void recoversPendingLineFromMatchingReceipt() {
        RewardSettlement settlement = createMixedSettlement(226102L);
        RewardSettlementLine itemLine = itemLine(settlement);
        inventoryDeliveryApi.deliverReward(
                itemLine.getId(), PLAYER_ID,
                itemLine.getItemCode(), itemLine.getAmount()
        );

        RewardSettlementItemProcessResult result = processService.process(
                settlement.getId(), itemLine.getId()
        );

        assertThat(result.replayed()).isTrue();
        assertThat(mailboxQuantity()).isEqualTo(1L);
        assertThat(receiptCount(itemLine.getId())).isEqualTo(1);
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
    }

    @Test
    @DisplayName("SUCCEEDED Line은 receipt-only replay하며 receipt 유실 시 신규 지급하지 않는다")
    void protectsSucceededLineWhenReceiptIsMissing() {
        RewardSettlement settlement = createMixedSettlement(226103L);
        RewardSettlementLine itemLine = itemLine(settlement);
        processService.process(settlement.getId(), itemLine.getId());
        jdbcTemplate.update(
                "DELETE FROM inventory_reward_deliveries WHERE reward_line_id = ?",
                itemLine.getId()
        );
        clearInvocations(inventoryDeliveryApi);

        assertThatThrownBy(() -> processService.process(
                settlement.getId(), itemLine.getId()
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        online.lifeasgame.reward.domain.error.RewardError
                                .REWARD_SETTLEMENT_ITEM_DELIVERY_INCONSISTENT
                )
        );

        verify(inventoryDeliveryApi, never()).deliverReward(
                itemLine.getId(), PLAYER_ID,
                itemLine.getItemCode(), itemLine.getAmount()
        );
        assertThat(mailboxQuantity()).isEqualTo(1L);
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.SUCCEEDED.name());
    }

    @Test
    @DisplayName("Mailbox full Domain failure는 delivery를 rollback하고 Line FAILED를 저장한다")
    void recordsMailboxFullWithoutPartialDelivery() {
        RewardSettlement settlement = createMixedSettlement(226104L);
        RewardSettlementLine itemLine = itemLine(settlement);
        RewardSettlementLine expLine = expLine(settlement);
        expProcessService.process(settlement.getId(), expLine.getId());
        insertFullMailbox();

        assertThatThrownBy(() -> processService.process(
                settlement.getId(), itemLine.getId()
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(InventoryError.MAILBOX_FULL)
        );

        assertThat(receiptCount(itemLine.getId())).isZero();
        assertThat(mailboxQuantity()).isEqualTo(99L);
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.FAILED.name());
        assertThat(lineFailureCode(itemLine.getId()))
                .isEqualTo(InventoryError.MAILBOX_FULL.code());
        assertThat(settlementStatus(settlement.getId()))
                .isEqualTo(RewardSettlementStatus.PARTIAL_FAILED.name());
    }

    @Test
    @DisplayName("Inventory system failure는 Line PENDING을 유지하고 전파한다")
    void propagatesInventorySystemFailure() {
        RewardSettlement settlement = createMixedSettlement(226105L);
        RewardSettlementLine itemLine = itemLine(settlement);
        doThrow(new RuntimeException("inventory system"))
                .when(inventoryDeliveryApi)
                .deliverReward(
                        itemLine.getId(), PLAYER_ID,
                        itemLine.getItemCode(), itemLine.getAmount()
                );

        assertThatThrownBy(() -> processService.process(
                settlement.getId(), itemLine.getId()
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("inventory system");

        assertThat(receiptCount(itemLine.getId())).isZero();
        assertThat(mailboxQuantity()).isZero();
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.PENDING.name());
    }

    @Test
    @DisplayName("Line save system failure는 Mailbox와 receipt까지 같은 Transaction에서 rollback한다")
    void rollsBackDeliveryWhenLineSaveFails() {
        RewardSettlement settlement = createMixedSettlement(226106L);
        RewardSettlementLine itemLine = itemLine(settlement);
        doThrow(new RuntimeException("line save system"))
                .when(settlementWriter)
                .saveAndFlush(argThat(candidate ->
                        settlement.getId().equals(candidate.getId())
                ));

        assertThatThrownBy(() -> processService.process(
                settlement.getId(), itemLine.getId()
        )).isInstanceOf(RuntimeException.class)
                .hasMessage("line save system");

        assertThat(receiptCount(itemLine.getId())).isZero();
        assertThat(mailboxQuantity()).isZero();
        assertThat(lineStatus(itemLine.getId()))
                .isEqualTo(RewardSettlementLineStatus.PENDING.name());
        assertThat(settlementStatus(settlement.getId()))
                .isEqualTo(RewardSettlementStatus.PENDING.name());
    }

    private RewardSettlement createMixedSettlement(long sourceId) {
        return createService.create(
                PLAYER_ID,
                RewardSettlementSourceType.QUEST_COMPLETION,
                sourceId,
                "RP_EXP_AND_ITEM_FIRST_STEP_20"
        );
    }

    private RewardSettlementLine itemLine(RewardSettlement settlement) {
        return settlement.getLines().stream()
                .filter(line -> line.getRewardType() == RewardType.ITEM)
                .findFirst()
                .orElseThrow();
    }

    private RewardSettlementLine expLine(RewardSettlement settlement) {
        return settlement.getLines().stream()
                .filter(line -> line.getRewardType() == RewardType.EXP)
                .findFirst()
                .orElseThrow();
    }

    private void insertPlayer() {
        jdbcTemplate.update("""
                INSERT INTO player (
                    id, user_id, name, gender, level, exp,
                    hp_cur, hp_cap, mp_cur, mp_cap,
                    str_stat, agi_stat, dex_stat, int_stat, vit_stat, luc_stat,
                    extra_stats, status_effects, version, created_at, updated_at
                ) VALUES (
                    ?, ?, 'ITEM Process Tester', 'male', 1, 0,
                    100, 100, 50, 50,
                    1, 1, 1, 1, 1, 1,
                    JSON_OBJECT(), '[]', 0,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, PLAYER_ID + 100000L);
    }

    private void insertFullMailbox() {
        Long itemId = jdbcTemplate.queryForObject(
                "SELECT id FROM items WHERE code = 'IT_FIRST_STEP_FRAGMENT'",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 1, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID);
        jdbcTemplate.update("""
                INSERT INTO mailbox_entries (
                    player_id, slot_index, item_id, rarity, quantity,
                    durability, bound, inst_attrs, created_at, updated_at
                ) VALUES (
                    ?, 0, ?, 'COMMON', 99,
                    NULL, TRUE, JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, itemId);
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
                firstFuture.get(
                        Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS
                ),
                secondFuture.get(
                        Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS
                )
        );
    }

    private long mailboxQuantity() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM mailbox_entries
                WHERE player_id = ?
                """, Long.class, PLAYER_ID);
    }

    private int receiptCount(Long lineId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_reward_deliveries
                WHERE reward_line_id = ?
                """, Integer.class, lineId);
    }

    private String lineStatus(Long lineId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_settlement_lines WHERE id = ?",
                String.class,
                lineId
        );
    }

    private String lineFailureCode(Long lineId) {
        return jdbcTemplate.queryForObject(
                "SELECT failure_code FROM reward_settlement_lines WHERE id = ?",
                String.class,
                lineId
        );
    }

    private String settlementStatus(Long settlementId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_settlements WHERE id = ?",
                String.class,
                settlementId
        );
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get();
    }
}
