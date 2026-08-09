package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mailbox claim concurrency MySQL integration")
class MailboxClaimConcurrencyIntegrationTest
        extends MailboxClaimMySqlIntegrationTestSupport {

    private static final Long PLAYER_ID = 228201L;
    private static final String ITEM_A = "IT_228_CONCURRENT_A";
    private static final String ITEM_B = "IT_228_CONCURRENT_B";

    @Autowired
    private MailboxService mailboxService;

    @Autowired
    private InventoryRewardDeliveryApi rewardDeliveryApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        resetState();
    }

    @Nested
    @DisplayName("같은 Mailbox slot을 동시에 claim하면")
    class ClaimSameSlot {

        @Test
        @DisplayName("quantity 1은 정확히 하나만 claim하고 timeout 없이 보존한다")
        void serializesSameSlotClaim() throws Exception {
            deliver(ITEM_A, 1, Map.of(), true);

            List<Throwable> outcomes = race(
                    () -> claim(0, 1),
                    () -> claim(0, 1)
            );

            assertOneFailure(outcomes, InventoryError.SLOT_EMPTY);
            assertThat(mailboxTotal()).isZero();
            assertThat(inventoryTotal()).isEqualTo(1);
            assertConserved(1, 0);
        }

        @Test
        @DisplayName("quantity 10의 두 claim 7은 하나만 성공한다")
        void serializesInsufficientSameSlotClaim() throws Exception {
            deliver(ITEM_A, 10, Map.of(), true);

            List<Throwable> outcomes = race(
                    () -> claim(0, 7),
                    () -> claim(0, 7)
            );

            assertOneFailure(outcomes, InventoryError.NOT_ENOUGH_QUANTITY);
            assertThat(mailboxTotal()).isEqualTo(3);
            assertThat(inventoryTotal()).isEqualTo(7);
            assertConserved(10, 0);
        }
    }

    @Nested
    @DisplayName("겹치는 claim과 claimAll을 동시에 실행하면")
    class ClaimOverlappingBatch {

        @Test
        @DisplayName("overlapping claimAll은 후행 batch 전체를 latest state에서 거부한다")
        void serializesOverlappingBatches() throws Exception {
            deliver(ITEM_A, 5, Map.of("grade", "A"), true);
            deliver(ITEM_A, 5, Map.of("grade", "B"), true);
            deliver(ITEM_B, 1, Map.of(), true);

            List<Throwable> outcomes = race(
                    () -> claimAll(
                            new MailboxCommand.Claim(0, 3),
                            new MailboxCommand.Claim(1, 3)
                    ),
                    () -> claimAll(
                            new MailboxCommand.Claim(1, 4),
                            new MailboxCommand.Claim(2, 1)
                    )
            );

            assertOneFailure(outcomes, InventoryError.NOT_ENOUGH_QUANTITY);
            assertThat(mailboxTotal() + inventoryTotal()).isEqualTo(11);
            if (inventoryTotal() == 6) {
                assertThat(mailboxSlotQuantity(0)).isEqualTo(2);
                assertThat(mailboxSlotQuantity(2)).isEqualTo(1);
            } else {
                assertThat(inventoryTotal()).isEqualTo(5);
                assertThat(mailboxSlotQuantity(0)).isEqualTo(5);
                assertThat(mailboxSlotQuantity(2)).isZero();
            }
        }

        @Test
        @DisplayName("claim과 claimAll도 partial batch 없이 직렬화한다")
        void serializesClaimAgainstBatch() throws Exception {
            deliver(ITEM_A, 2, Map.of("grade", "A"), true);
            deliver(ITEM_A, 1, Map.of("grade", "B"), true);

            List<Throwable> outcomes = race(
                    () -> claim(0, 1),
                    () -> claimAll(
                            new MailboxCommand.Claim(0, 2),
                            new MailboxCommand.Claim(1, 1)
                    )
            );

            assertThat(successCount(outcomes)).isEqualTo(1);
            assertThat(failure(outcomes)).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode()).isIn(
                            InventoryError.SLOT_EMPTY,
                            InventoryError.NOT_ENOUGH_QUANTITY
                    )
            );
            assertThat(mailboxTotal() + inventoryTotal()).isEqualTo(3);
            if (inventoryTotal() == 1) {
                assertThat(mailboxSlotQuantity(1)).isEqualTo(1);
            } else {
                assertThat(inventoryTotal()).isEqualTo(3);
                assertThat(mailboxTotal()).isZero();
            }
        }
    }

    @Nested
    @DisplayName("claim과 다른 Mailbox mutation이 경쟁하면")
    class ClaimAgainstOtherMutation {

        @RepeatedTest(10)
        @DisplayName("Reward delivery와 lost update 없이 receipt와 총량을 보존한다")
        void serializesClaimAgainstRewardDelivery() throws Exception {
            deliver(ITEM_A, 1, Map.of(), true);

            List<Throwable> outcomes = race(
                    () -> claim(0, 1),
                    () -> rewardDeliveryApi.deliverReward(
                            2282001L,
                            PLAYER_ID,
                            ITEM_A,
                            2L
                    )
            );

            assertThat(outcomes).containsOnlyNulls();
            assertThat(receiptCount(2282001L)).isEqualTo(1);
            assertThat(mailboxTotal()).isEqualTo(2);
            assertThat(inventoryTotal()).isEqualTo(1);
            assertConserved(3, 0);
        }

        @Test
        @DisplayName("delete와 정확히 하나만 적용하고 총량을 보존한다")
        void serializesClaimAgainstDelete() throws Exception {
            deliver(ITEM_A, 1, Map.of(), true);

            List<Throwable> outcomes = race(
                    () -> claim(0, 1),
                    () -> mailboxService.delete(
                            PLAYER_ID,
                            new MailboxCommand.Delete(0)
                    )
            );

            assertOneFailure(outcomes, InventoryError.SLOT_EMPTY);
            long deleted = inventoryTotal() == 0 ? 1 : 0;
            assertThat(mailboxTotal()).isZero();
            assertConserved(1, deleted);
        }
    }

    private void claim(int slot, int quantity) {
        mailboxService.claim(
                PLAYER_ID,
                new MailboxCommand.Claim(slot, quantity)
        );
    }

    private void claimAll(MailboxCommand.Claim... claims) {
        mailboxService.claimAll(
                PLAYER_ID,
                new MailboxCommand.ClaimAll(List.of(claims))
        );
    }

    private void deliver(
            String itemCode,
            int quantity,
            Map<String, Object> attrs,
            boolean bound
    ) {
        mailboxService.deliver(
                PLAYER_ID,
                new MailboxCommand.Deliver(
                        itemId(itemCode),
                        quantity,
                        attrs,
                        bound
                )
        );
    }

    private List<Throwable> race(
            ThrowingAction first,
            ThrowingAction second
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();

        try {
            for (ThrowingAction action : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        action.run();
                        return null;
                    } catch (Throwable failure) {
                        return failure;
                    }
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : futures) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertOneFailure(
            List<Throwable> outcomes,
            InventoryError error
    ) {
        assertThat(successCount(outcomes)).isEqualTo(1);
        assertThat(failure(outcomes)).isInstanceOfSatisfying(
                DomainException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(error)
        );
    }

    private long successCount(List<Throwable> outcomes) {
        return outcomes.stream().filter(Objects::isNull).count();
    }

    private Throwable failure(List<Throwable> outcomes) {
        return outcomes.stream().filter(Objects::nonNull).findFirst()
                .orElseThrow();
    }

    private void assertConserved(long initialAndDelivered, long deleted) {
        assertThat(mailboxTotal() + inventoryTotal() + deleted)
                .isEqualTo(initialAndDelivered);
    }

    private void resetState() {
        jdbcTemplate.update("DELETE FROM inventory_reward_deliveries");
        jdbcTemplate.update("DELETE FROM inventory_entries");
        jdbcTemplate.update("DELETE FROM mailbox_entries");
        jdbcTemplate.update("DELETE FROM player_inventory");
        jdbcTemplate.update("DELETE FROM player_mailbox");
        insertItem(ITEM_A, "Issue 228 concurrent A");
        insertItem(ITEM_B, "Issue 228 concurrent B");
        jdbcTemplate.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID);
        jdbcTemplate.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID);
    }

    private void insertItem(String code, String name) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    ?, ?, 'QUEST', 'ETC', 'COMMON', JSON_OBJECT(),
                    TRUE, 10, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, code, name);
    }

    private Long itemId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM items WHERE code = ?",
                Long.class,
                code
        );
    }

    private long mailboxTotal() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM mailbox_entries
                WHERE player_id = ?
                """, Long.class, PLAYER_ID);
    }

    private int mailboxSlotQuantity(int slot) {
        return jdbcTemplate.queryForList("""
                SELECT quantity
                FROM mailbox_entries
                WHERE player_id = ? AND slot_index = ?
                """, Integer.class, PLAYER_ID, slot)
                .stream()
                .findFirst()
                .orElse(0);
    }

    private long inventoryTotal() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM inventory_entries
                WHERE player_id = ?
                """, Long.class, PLAYER_ID);
    }

    private int receiptCount(Long rewardLineId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_reward_deliveries
                WHERE reward_line_id = ?
                """, Integer.class, rewardLineId);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
