package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryMarketAvailabilityApi;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Inventory market availability MySQL integration")
class InventoryMarketAvailabilityConcurrencyIntegrationTest
        extends MailboxClaimMySqlIntegrationTestSupport {

    private static final Long PLAYER_ID = 294001L;
    private static final Long OTHER_PLAYER_ID = 294002L;
    private static final String ITEM_CODE = "IT_294_MARKET";

    @Autowired
    private InventoryMarketAvailabilityApi availabilityApi;

    @Autowired
    private JdbcTemplate jdbc;

    private Long itemId;
    private Long inventoryEntryId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM player_equipment");
        jdbc.update("DELETE FROM inventory_entries");
        jdbc.update("DELETE FROM player_inventory");
        jdbc.update("DELETE FROM items WHERE code = ?", ITEM_CODE);
        itemId = insertItem();
        insertInventory(PLAYER_ID);
        insertInventory(OTHER_PLAYER_ID);
        inventoryEntryId = insertEntry(PLAYER_ID, 7);
    }

    @Nested
    @DisplayName("whole entry의 market availability를 변경할 때")
    class WholeEntryTransitions {

        @Test
        @DisplayName("owner와 전체 current quantity snapshot을 보존하며 확정 전이를 적용한다")
        void transitionsAndReturnsWholeEntrySnapshot() {
            var listed = availabilityApi.listWholeEntry(
                    PLAYER_ID,
                    inventoryEntryId
            );

            assertThat(listed).isEqualTo(
                    new InventoryMarketAvailabilityApi.EntrySnapshot(
                            inventoryEntryId,
                            PLAYER_ID,
                            itemId,
                            7,
                            "LISTED"
                    )
            );
            assertThat(availabilityApi.reserveForTrade(
                    PLAYER_ID,
                    inventoryEntryId
            ).availability()).isEqualTo("RESERVED_FOR_TRADE");
            assertThat(availabilityApi.releaseTradeReservation(
                    PLAYER_ID,
                    inventoryEntryId
            ).availability()).isEqualTo("LISTED");
            assertThat(availabilityApi.releaseListing(
                    PLAYER_ID,
                    inventoryEntryId
            ).availability()).isEqualTo("FREE");
            availabilityApi.listWholeEntry(PLAYER_ID, inventoryEntryId);
            availabilityApi.reserveForTrade(PLAYER_ID, inventoryEntryId);
            assertThat(availabilityApi.beginTransfer(
                    PLAYER_ID,
                    inventoryEntryId
            )).isEqualTo(
                    new InventoryMarketAvailabilityApi.EntrySnapshot(
                            inventoryEntryId,
                            PLAYER_ID,
                            itemId,
                            7,
                            "TRANSFER_PROCESSING"
                    )
            );
            assertThat(availabilityApi.getSnapshot(
                    PLAYER_ID,
                    inventoryEntryId
            ).quantity()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("같은 whole entry를 동시에 listing 점유할 때")
    class ConcurrentListing {

        @Test
        @DisplayName("DB parent lock으로 하나만 성공하고 durable state는 LISTED다")
        void serializesDuplicateOccupation() throws Exception {
            List<Throwable> outcomes = race(
                    () -> availabilityApi.listWholeEntry(
                            PLAYER_ID,
                            inventoryEntryId
                    ),
                    () -> availabilityApi.listWholeEntry(
                            PLAYER_ID,
                            inventoryEntryId
                    )
            );

            assertThat(outcomes.stream().filter(Objects::isNull).count())
                    .isEqualTo(1);
            assertThat(outcomes.stream().filter(Objects::nonNull).findFirst())
                    .hasValueSatisfying(failure -> assertThat(failure)
                            .isInstanceOfSatisfying(
                                    DomainException.class,
                                    exception -> assertThat(
                                            exception.getErrorCode()
                                    ).isEqualTo(
                                            InventoryError.INVALID_AVAILABILITY_TRANSITION
                                    )
                            ));
            assertThat(jdbc.queryForObject("""
                    SELECT availability
                    FROM inventory_entries
                    WHERE id = ?
                    """, String.class, inventoryEntryId)).isEqualTo("LISTED");
        }
    }

    private List<Throwable> race(Runnable first, Runnable second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Throwable>> futures = new ArrayList<>();
        try {
            for (Runnable action : List.of(first, second)) {
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

    private Long insertItem() {
        jdbc.update("""
                INSERT INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    ?, 'Issue 294 market item', 'QUEST', 'ETC',
                    'COMMON', JSON_OBJECT(), TRUE, 10, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, ITEM_CODE);
        return jdbc.queryForObject(
                "SELECT id FROM items WHERE code = ?",
                Long.class,
                ITEM_CODE
        );
    }

    private void insertInventory(Long playerId) {
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version,
                    created_at, updated_at
                ) VALUES (
                    ?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, playerId);
    }

    private Long insertEntry(Long playerId, int quantity) {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity
                ) VALUES (
                    FALSE, NULL, ?, 0,
                    CURRENT_TIMESTAMP(6), ?, ?, CURRENT_TIMESTAMP(6),
                    JSON_OBJECT(), 'COMMON'
                )
                """, quantity, itemId, playerId);
        return jdbc.queryForObject("""
                SELECT id
                FROM inventory_entries
                WHERE player_id = ? AND item_id = ?
                """, Long.class, playerId, itemId);
    }
}
