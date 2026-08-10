package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.error.ErrorCode;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Mailbox claim MySQL integration")
class MailboxClaimIntegrationTest
        extends MailboxClaimMySqlIntegrationTestSupport {

    private static final Long PLAYER_ID = 228101L;
    private static final String ITEM_A = "IT_228_STACK_A";
    private static final String ITEM_B = "IT_228_STACK_B";

    @Autowired
    private MailboxService mailboxService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanState();
        ensureItems();
        insertContainers(3, 10);
    }

    @Nested
    @DisplayName("Mailbox 아이템을 Inventory로 수령할 때")
    class ClaimSuccessfully {

        @Test
        @DisplayName("single claim은 attrs/bound를 보존하며 stack merge와 신규 stack을 적용한다")
        void claimsSingleAfterCompletePreflight() {
            insertInventoryEntry(0, itemId(ITEM_A), 9, true, "A");
            mailboxService.deliver(
                    PLAYER_ID,
                    new MailboxCommand.Deliver(
                            itemId(ITEM_A),
                            5,
                            Map.of("grade", "A"),
                            true
                    )
            );

            mailboxService.claim(
                    PLAYER_ID,
                    new MailboxCommand.Claim(0, 5)
            );

            assertThat(mailboxTotal()).isZero();
            assertThat(inventoryQuantities()).containsExactly(4, 10);
            assertThat(inventoryBoundCount()).isEqualTo(2);
            assertThat(inventoryGradeCount("A")).isEqualTo(2);
        }

        @Test
        @DisplayName("claimAll은 서로 다른 stack key를 한 batch로 옮긴다")
        void claimsAllAfterBatchPreflight() {
            mailboxService.deliver(
                    PLAYER_ID,
                    new MailboxCommand.Deliver(
                            itemId(ITEM_A),
                            5,
                            Map.of("grade", "A"),
                            true
                    )
            );
            mailboxService.deliver(
                    PLAYER_ID,
                    new MailboxCommand.Deliver(
                            itemId(ITEM_A),
                            4,
                            Map.of("grade", "B"),
                            false
                    )
            );

            mailboxService.claimAll(
                    PLAYER_ID,
                    new MailboxCommand.ClaimAll(List.of(
                            new MailboxCommand.Claim(0, 2),
                            new MailboxCommand.Claim(1, 3)
                    ))
            );

            assertThat(mailboxQuantities()).containsExactly(3, 1);
            assertThat(inventoryTotal()).isEqualTo(5);
            assertThat(inventoryEntryCount()).isEqualTo(2);
            assertThat(inventoryGradeCount("A")).isEqualTo(1);
            assertThat(inventoryGradeCount("B")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("claim preflight가 실패하면")
    class PreserveAggregatesOnFailure {

        @Test
        @DisplayName("single Inventory full은 Mailbox와 Inventory를 모두 유지한다")
        void keepsBothAggregatesOnSingleFailure() {
            cleanState();
            ensureItems();
            insertContainers(1, 10);
            insertInventoryEntry(0, itemId(ITEM_A), 1, false, null);
            mailboxService.deliver(
                    PLAYER_ID,
                    new MailboxCommand.Deliver(
                            itemId(ITEM_A),
                            1,
                            Map.of(),
                            true
                    )
            );
            State before = state();

            assertError(
                    () -> mailboxService.claim(
                            PLAYER_ID,
                            new MailboxCommand.Claim(0, 1)
                    ),
                    InventoryError.INVENTORY_FULL
            );

            assertThat(state()).isEqualTo(before);
        }

        @Test
        @DisplayName("batch 마지막 quantity 실패도 앞선 Mailbox를 차감하지 않는다")
        void keepsBothAggregatesOnLateQuantityFailure() {
            deliverTwoEntries();
            State before = state();

            assertError(
                    () -> mailboxService.claimAll(
                            PLAYER_ID,
                            new MailboxCommand.ClaimAll(List.of(
                                    new MailboxCommand.Claim(0, 1),
                                    new MailboxCommand.Claim(1, 2)
                            ))
                    ),
                    InventoryError.NOT_ENOUGH_QUANTITY
            );

            assertThat(state()).isEqualTo(before);
        }

        @Test
        @DisplayName("batch 중간 empty와 마지막 missing Item도 두 aggregate를 유지한다")
        void keepsBothAggregatesOnEmptyOrMissingItem() {
            insertMailboxEntry(0, itemId(ITEM_A), 1, true);
            State beforeEmpty = state();

            assertError(
                    () -> mailboxService.claimAll(
                            PLAYER_ID,
                            new MailboxCommand.ClaimAll(List.of(
                                    new MailboxCommand.Claim(0, 1),
                                    new MailboxCommand.Claim(1, 1)
                            ))
                    ),
                    InventoryError.SLOT_EMPTY
            );
            assertThat(state()).isEqualTo(beforeEmpty);

            insertMailboxEntry(1, 9_999_999L, 1, true);
            State beforeMissing = state();
            assertError(
                    () -> mailboxService.claimAll(
                            PLAYER_ID,
                            new MailboxCommand.ClaimAll(List.of(
                                    new MailboxCommand.Claim(0, 1),
                                    new MailboxCommand.Claim(1, 1)
                            ))
                    ),
                    ItemError.ITEM_NOT_FOUND
            );
            assertThat(state()).isEqualTo(beforeMissing);
        }

        @Test
        @DisplayName("누적 capacity 실패는 batch의 Mailbox와 Inventory를 모두 유지한다")
        void keepsBothAggregatesOnAccumulatedCapacityFailure() {
            cleanState();
            ensureItems();
            insertContainers(2, 10);
            insertInventoryEntry(0, itemId(ITEM_A), 9, true, null);
            insertMailboxEntry(0, itemId(ITEM_A), 10, true);
            insertMailboxEntry(1, itemId(ITEM_B), 1, true);
            State before = state();

            assertError(
                    () -> mailboxService.claimAll(
                            PLAYER_ID,
                            new MailboxCommand.ClaimAll(List.of(
                                    new MailboxCommand.Claim(0, 10),
                                    new MailboxCommand.Claim(1, 1)
                            ))
                    ),
                    InventoryError.INVENTORY_FULL
            );

            assertThat(state()).isEqualTo(before);
        }
    }

    private void deliverTwoEntries() {
        mailboxService.deliver(
                PLAYER_ID,
                new MailboxCommand.Deliver(
                        itemId(ITEM_A),
                        1,
                        Map.of("grade", "A"),
                        true
                )
        );
        mailboxService.deliver(
                PLAYER_ID,
                new MailboxCommand.Deliver(
                        itemId(ITEM_A),
                        1,
                        Map.of("grade", "B"),
                        true
                )
        );
    }

    private void ensureItems() {
        insertItem(ITEM_A, "Issue 228 stack A");
        insertItem(ITEM_B, "Issue 228 stack B");
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

    private void insertContainers(int inventoryCapacity, int mailboxCapacity) {
        jdbcTemplate.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID, inventoryCapacity);
        jdbcTemplate.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, PLAYER_ID, mailboxCapacity);
    }

    private void insertMailboxEntry(
            int slot,
            Long itemId,
            int quantity,
            boolean bound
    ) {
        jdbcTemplate.update("""
                INSERT INTO mailbox_entries (
                    player_id, slot_index, item_id, rarity, quantity,
                    durability, bound, inst_attrs, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, 'COMMON', ?, NULL, ?, JSON_OBJECT(),
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, slot, itemId, quantity, bound);
    }

    private void insertInventoryEntry(
            int slot,
            Long itemId,
            int quantity,
            boolean bound,
            String grade
    ) {
        jdbcTemplate.update("""
                INSERT INTO inventory_entries (
                    player_id, slot_index, item_id, rarity, quantity,
                    durability, bound, inst_attrs, created_at, updated_at
                ) VALUES (
                    ?, ?, ?, 'COMMON', ?, NULL, ?,
                    IF(? IS NULL, JSON_OBJECT(), JSON_OBJECT('grade', ?)),
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, slot, itemId, quantity, bound, grade, grade);
    }

    private void cleanState() {
        jdbcTemplate.update("DELETE FROM inventory_reward_deliveries");
        jdbcTemplate.update("DELETE FROM inventory_entries");
        jdbcTemplate.update("DELETE FROM mailbox_entries");
        jdbcTemplate.update("DELETE FROM player_inventory");
        jdbcTemplate.update("DELETE FROM player_mailbox");
    }

    private Long itemId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM items WHERE code = ?",
                Long.class,
                code
        );
    }

    private State state() {
        return new State(
                mailboxEntryCount(),
                mailboxTotal(),
                containerVersion("player_mailbox"),
                inventoryEntryCount(),
                inventoryTotal(),
                containerVersion("player_inventory")
        );
    }

    private long containerVersion(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM " + table + " WHERE player_id = ?",
                Long.class,
                PLAYER_ID
        );
    }

    private int mailboxEntryCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mailbox_entries WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private long mailboxTotal() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM mailbox_entries
                WHERE player_id = ?
                """, Long.class, PLAYER_ID);
    }

    private List<Integer> mailboxQuantities() {
        return jdbcTemplate.queryForList("""
                SELECT quantity
                FROM mailbox_entries
                WHERE player_id = ?
                ORDER BY slot_index
                """, Integer.class, PLAYER_ID);
    }

    private int inventoryEntryCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_entries WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private long inventoryTotal() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM inventory_entries
                WHERE player_id = ?
                """, Long.class, PLAYER_ID);
    }

    private List<Integer> inventoryQuantities() {
        return jdbcTemplate.queryForList("""
                SELECT quantity
                FROM inventory_entries
                WHERE player_id = ?
                ORDER BY quantity
                """, Integer.class, PLAYER_ID);
    }

    private int inventoryBoundCount() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_entries
                WHERE player_id = ? AND bound = TRUE
                """, Integer.class, PLAYER_ID);
    }

    private int inventoryGradeCount(String grade) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM inventory_entries
                WHERE player_id = ?
                  AND JSON_UNQUOTE(JSON_EXTRACT(inst_attrs, '$.grade')) = ?
                """, Integer.class, PLAYER_ID, grade);
    }

    private void assertError(Runnable call, ErrorCode error) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }

    private record State(
            int mailboxEntries,
            long mailboxQuantity,
            long mailboxVersion,
            int inventoryEntries,
            long inventoryQuantity,
            long inventoryVersion
    ) {
    }
}
