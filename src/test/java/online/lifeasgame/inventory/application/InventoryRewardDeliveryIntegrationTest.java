package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Inventory reward delivery integration")
class InventoryRewardDeliveryIntegrationTest {

    private static final Long PLAYER_ID = 224101L;
    private static final Long OTHER_PLAYER_ID = 224102L;
    private static final String ITEM_CODE = "IT_FIRST_STEP_FRAGMENT";
    private static final String OTHER_ITEM_CODE = "IT_REWARD_SECOND";

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_inventory_reward_delivery")
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
    private InventoryRewardDeliveryApi deliveryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private Environment environment;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDeliveryState() {
        jdbcTemplate.update("DELETE FROM inventory_reward_deliveries");
        jdbcTemplate.update("DELETE FROM inventory_entries");
        jdbcTemplate.update("DELETE FROM mailbox_entries");
        jdbcTemplate.update("DELETE FROM player_inventory");
        jdbcTemplate.update("DELETE FROM player_mailbox");
        jdbcTemplate.update("""
                INSERT IGNORE INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    ?, 'Reward test item', 'QUEST', 'ETC', 'COMMON',
                    JSON_OBJECT(), TRUE, 10, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, OTHER_ITEM_CODE);
    }

    @Test
    @DisplayName("V17 receipt table은 JPA validate와 unique/check/index 계약을 만족한다")
    void validatesMigrationContract() {
        assertThat(flyway.info().current().getVersion().getVersion())
                .isEqualTo("32");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        Set<String> constraints = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_reward_deliveries'
                """, String.class));
        assertThat(constraints).contains(
                "uq_inventory_reward_delivery_line",
                "ck_inventory_reward_delivery_line",
                "ck_inventory_reward_delivery_player",
                "ck_inventory_reward_delivery_item",
                "ck_inventory_reward_delivery_quantity",
                "ck_inventory_reward_delivery_item_code"
        );

        Set<String> indexes = Set.copyOf(jdbcTemplate.queryForList("""
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_reward_deliveries'
                """, String.class));
        assertThat(indexes).contains(
                "uq_inventory_reward_delivery_line",
                "idx_inventory_reward_delivery_player"
        );
    }

    @Nested
    @DisplayName("Reward를 지급하거나 receipt를 조회할 때")
    class DeliverAndReadReceipt {

        @Test
        @DisplayName("stable ItemCode로 bound=true, empty attrs 지급과 receipt를 원자 저장한다")
        void deliversNewReward() {
            InventoryRewardDeliveryApi.RewardDeliveryResult result =
                    deliveryApi.deliverReward(
                            2241001L,
                            PLAYER_ID,
                            "  " + ITEM_CODE + "  ",
                            2L
                    );

            assertThat(result.deliveryId()).isPositive();
            assertThat(result.rewardLineId()).isEqualTo(2241001L);
            assertThat(result.playerId()).isEqualTo(PLAYER_ID);
            assertThat(result.itemId()).isEqualTo(itemId(ITEM_CODE));
            assertThat(result.itemCode()).isEqualTo(ITEM_CODE);
            assertThat(result.quantity()).isEqualTo(2L);
            assertThat(result.replayed()).isFalse();
            assertThat(receiptCount(2241001L)).isEqualTo(1);
            assertThat(mailboxTotalQuantity(PLAYER_ID)).isEqualTo(2L);
            assertThat(mailboxEntryBound(PLAYER_ID)).isTrue();
            assertThat(mailboxEntryAttrs(PLAYER_ID)).isEqualTo("{}");

            InventoryRewardDeliveryApi.RewardDeliveryReceipt receipt =
                    deliveryApi.findRewardDelivery(2241001L).orElseThrow();
            assertThat(receipt.deliveryId()).isEqualTo(result.deliveryId());
            assertThat(receipt.rewardLineId()).isEqualTo(2241001L);
            assertThat(receipt.playerId()).isEqualTo(PLAYER_ID);
            assertThat(receipt.itemId()).isEqualTo(result.itemId());
            assertThat(receipt.itemCode()).isEqualTo(ITEM_CODE);
            assertThat(receipt.quantity()).isEqualTo(2L);
        }

        @Test
        @DisplayName("receipt 조회 miss는 container, Mailbox, Catalog를 변경하지 않는다")
        void findsMissingReceiptWithoutMutation() {
            assertThat(deliveryApi.findRewardDelivery(2241099L)).isEmpty();

            assertThat(receiptCountAll()).isZero();
            assertThat(containerCount("player_inventory", PLAYER_ID)).isZero();
            assertThat(containerCount("player_mailbox", PLAYER_ID)).isZero();
            assertThat(mailboxEntryCount(PLAYER_ID)).isZero();
        }

        @Test
        @DisplayName("receipt 조회도 rewardLineId 양수 경계를 검증한다")
        void validatesReceiptLookupRewardLineId() {
            assertError(
                    () -> deliveryApi.findRewardDelivery(0L),
                    InventoryError.REWARD_LINE_ID_INVALID
            );
        }

        @Test
        @DisplayName("기존 stack, 신규 stack, 다중 stack에 지급하고 receipt는 전체 수량을 저장한다")
        void stacksAcrossMailboxEntries() {
            deliveryApi.deliverReward(2241101L, PLAYER_ID, ITEM_CODE, 90L);
            deliveryApi.deliverReward(2241102L, PLAYER_ID, ITEM_CODE, 30L);

            assertThat(mailboxQuantities(PLAYER_ID)).containsExactly(21, 99);
            assertThat(receiptQuantity(2241102L)).isEqualTo(30L);

            deliveryApi.deliverReward(2241103L, PLAYER_ID, ITEM_CODE, 150L);

            assertThat(mailboxQuantities(PLAYER_ID)).containsExactly(72, 99, 99);
            assertThat(mailboxTotalQuantity(PLAYER_ID)).isEqualTo(270L);
            assertThat(receiptQuantity(2241103L)).isEqualTo(150L);
        }
    }

    @Nested
    @DisplayName("동일 rewardLineId가 재전달되면")
    class ReplayDelivery {

        @Test
        @DisplayName("순차 동일 payload replay는 Mailbox를 다시 변경하지 않는다")
        void replaysSequentially() {
            InventoryRewardDeliveryApi.RewardDeliveryResult first =
                    deliveryApi.deliverReward(2241201L, PLAYER_ID, ITEM_CODE, 5L);
            InventoryRewardDeliveryApi.RewardDeliveryResult replay =
                    deliveryApi.deliverReward(
                            2241201L,
                            PLAYER_ID,
                            "  " + ITEM_CODE + " ",
                            5L
                    );

            assertThat(first.replayed()).isFalse();
            assertThat(replay.replayed()).isTrue();
            assertThat(replay.deliveryId()).isEqualTo(first.deliveryId());
            assertThat(mailboxTotalQuantity(PLAYER_ID)).isEqualTo(5L);
            assertThat(receiptCount(2241201L)).isEqualTo(1);
        }

        @Test
        @DisplayName("동시 동일 payload는 Mailbox lock으로 한 번만 지급한다")
        void replaysConcurrently() throws Exception {
            int workers = 2;
            ExecutorService executor = Executors.newFixedThreadPool(workers);
            CountDownLatch ready = new CountDownLatch(workers);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<InventoryRewardDeliveryApi.RewardDeliveryResult>> futures =
                    new ArrayList<>();

            try {
                for (int i = 0; i < workers; i++) {
                    futures.add(executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return deliveryApi.deliverReward(
                                2241301L,
                                PLAYER_ID,
                                ITEM_CODE,
                                7L
                        );
                    }));
                }

                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                List<InventoryRewardDeliveryApi.RewardDeliveryResult> results =
                        new ArrayList<>();
                for (Future<InventoryRewardDeliveryApi.RewardDeliveryResult> future : futures) {
                    results.add(future.get(30, TimeUnit.SECONDS));
                }

                assertThat(results)
                        .extracting(InventoryRewardDeliveryApi.RewardDeliveryResult::replayed)
                        .containsExactlyInAnyOrder(false, true);
                assertThat(results)
                        .extracting(InventoryRewardDeliveryApi.RewardDeliveryResult::deliveryId)
                        .containsOnly(results.getFirst().deliveryId());
            } finally {
                executor.shutdownNow();
            }

            assertThat(mailboxTotalQuantity(PLAYER_ID)).isEqualTo(7L);
            assertThat(receiptCount(2241301L)).isEqualTo(1);
        }

        @Test
        @DisplayName("다른 player, itemCode, quantity는 stable conflict다")
        void rejectsReplayPayloadMismatch() {
            deliveryApi.deliverReward(2241401L, PLAYER_ID, ITEM_CODE, 3L);

            assertConflict(() -> deliveryApi.deliverReward(
                    2241401L,
                    OTHER_PLAYER_ID,
                    ITEM_CODE,
                    3L
            ));
            assertConflict(() -> deliveryApi.deliverReward(
                    2241401L,
                    PLAYER_ID,
                    OTHER_ITEM_CODE,
                    3L
            ));
            assertConflict(() -> deliveryApi.deliverReward(
                    2241401L,
                    PLAYER_ID,
                    ITEM_CODE,
                    4L
            ));

            assertThat(mailboxTotalQuantity(PLAYER_ID)).isEqualTo(3L);
            assertThat(mailboxTotalQuantity(OTHER_PLAYER_ID)).isZero();
            assertThat(receiptCount(2241401L)).isEqualTo(1);
            assertThat(receiptQuantity(2241401L)).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Reward를 지급할 수 없으면")
    class RejectOrRollbackDelivery {

        @Test
        @DisplayName("invalid input과 missing Item은 receipt와 Mailbox를 변경하지 않는다")
        void rejectsInvalidOrMissingPayload() {
            assertError(
                    () -> deliveryApi.deliverReward(0L, PLAYER_ID, ITEM_CODE, 1L),
                    InventoryError.REWARD_LINE_ID_INVALID
            );
            assertError(
                    () -> deliveryApi.deliverReward(2241501L, 0L, ITEM_CODE, 1L),
                    InventoryError.PLAYER_ID_INVALID
            );
            assertError(
                    () -> deliveryApi.deliverReward(2241502L, PLAYER_ID, " ", 1L),
                    InventoryError.REWARD_ITEM_CODE_INVALID
            );
            assertError(
                    () -> deliveryApi.deliverReward(
                            2241503L,
                            PLAYER_ID,
                            "I".repeat(81),
                            1L
                    ),
                    InventoryError.REWARD_ITEM_CODE_INVALID
            );
            assertError(
                    () -> deliveryApi.deliverReward(2241504L, PLAYER_ID, ITEM_CODE, 0L),
                    InventoryError.REWARD_QUANTITY_INVALID
            );
            assertError(
                    () -> deliveryApi.deliverReward(
                            2241505L,
                            PLAYER_ID,
                            ITEM_CODE,
                            (long) Integer.MAX_VALUE + 1
                    ),
                    InventoryError.REWARD_QUANTITY_INVALID
            );
            assertItemError(() -> deliveryApi.deliverReward(
                    2241506L,
                    PLAYER_ID,
                    "IT_MISSING",
                    1L
            ));

            assertThat(receiptCountAll()).isZero();
            assertThat(mailboxTotalQuantity(PLAYER_ID)).isZero();
        }

        @Test
        @DisplayName("Mailbox full은 entry mutation과 receipt를 모두 rollback한다")
        void rollsBackWhenMailboxIsFull() {
            insertInventory(PLAYER_ID, PlayerInventory.DEFAULT_CAPACITY);
            insertMailbox(PLAYER_ID, 1);

            assertError(
                    () -> deliveryApi.deliverReward(
                            2241601L,
                            PLAYER_ID,
                            ITEM_CODE,
                            100L
                    ),
                    InventoryError.MAILBOX_FULL
            );

            assertThat(mailboxTotalQuantity(PLAYER_ID)).isZero();
            assertThat(mailboxEntryCount(PLAYER_ID)).isZero();
            assertThat(receiptCount(2241601L)).isZero();
            assertThat(mailboxCapacity(PLAYER_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("provider는 caller REQUIRED transaction rollback에 참여한다")
        void joinsCallerTransaction() {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);

            transaction.executeWithoutResult(status -> {
                deliveryApi.deliverReward(2241701L, PLAYER_ID, ITEM_CODE, 1L);
                status.setRollbackOnly();
            });

            assertThat(receiptCount(2241701L)).isZero();
            assertThat(mailboxEntryCount(PLAYER_ID)).isZero();
            assertThat(containerCount("player_inventory", PLAYER_ID)).isZero();
            assertThat(containerCount("player_mailbox", PLAYER_ID)).isZero();
        }
    }

    private void assertConflict(Runnable call) {
        assertError(call, InventoryError.REWARD_DELIVERY_CONFLICT);
    }

    private void assertItemError(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ItemError.ITEM_CODE_NOT_FOUND)
                );
    }

    private void assertError(Runnable call, InventoryError error) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }

    private void insertInventory(Long playerId, int capacity) {
        jdbcTemplate.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, capacity);
    }

    private void insertMailbox(Long playerId, int capacity) {
        jdbcTemplate.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, capacity);
    }

    private Long itemId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM items WHERE code = ?",
                Long.class,
                code
        );
    }

    private int receiptCount(Long rewardLineId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_reward_deliveries
                WHERE reward_line_id = ?
                """, Integer.class, rewardLineId);
    }

    private int receiptCountAll() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_reward_deliveries",
                Integer.class
        );
    }

    private long receiptQuantity(Long rewardLineId) {
        return jdbcTemplate.queryForObject("""
                SELECT quantity
                FROM inventory_reward_deliveries
                WHERE reward_line_id = ?
                """, Long.class, rewardLineId);
    }

    private int mailboxEntryCount(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mailbox_entries WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }

    private long mailboxTotalQuantity(Long playerId) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM mailbox_entries
                WHERE player_id = ?
                """, Long.class, playerId);
    }

    private List<Integer> mailboxQuantities(Long playerId) {
        return jdbcTemplate.queryForList("""
                SELECT quantity
                FROM mailbox_entries
                WHERE player_id = ?
                ORDER BY quantity, slot_index
                """, Integer.class, playerId);
    }

    private boolean mailboxEntryBound(Long playerId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT bound
                FROM mailbox_entries
                WHERE player_id = ?
                """, Boolean.class, playerId));
    }

    private String mailboxEntryAttrs(Long playerId) {
        return jdbcTemplate.queryForObject("""
                SELECT CAST(inst_attrs AS CHAR)
                FROM mailbox_entries
                WHERE player_id = ?
                """, String.class, playerId);
    }

    private int mailboxCapacity(Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT capacity_slots FROM player_mailbox WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }

    private int containerCount(String table, Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }
}
