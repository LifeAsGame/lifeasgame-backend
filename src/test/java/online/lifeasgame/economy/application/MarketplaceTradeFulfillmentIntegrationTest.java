package online.lifeasgame.economy.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Marketplace whole-entry fulfillment MySQL integration")
class MarketplaceTradeFulfillmentIntegrationTest {

    private static final long SELLER_ID = 298001L;
    private static final long BUYER_ID = 298002L;
    private static final String ITEM_CODE = "IT_298_MARKETPLACE";
    private static final String RESERVATION_TOKEN = "reservation-298";
    private static final String HOLD_ID = "hold-298";

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            "mysql:8.0.39"
    )
            .withDatabaseName("lifeasgame_marketplace_298")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
    }

    @Autowired
    private MarketplaceService marketplaceService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Flyway flyway;

    @MockitoBean
    private IdempotencyKeyStore idempotencyKeyStore;

    private Long itemId;
    private Long sourceEntryId;
    private Long listingId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM trades");
        jdbc.update("DELETE FROM listing_reservations");
        jdbc.update("DELETE FROM wallet_holds");
        jdbc.update("DELETE FROM wallet_balances");
        jdbc.update("DELETE FROM wallets");
        jdbc.update("DELETE FROM listings");
        jdbc.update("DELETE FROM player_equipment");
        jdbc.update("DELETE FROM inventory_entries");
        jdbc.update("DELETE FROM player_inventory");
        jdbc.update("DELETE FROM items WHERE code = ?", ITEM_CODE);

        itemId = insertItem();
        insertInventory(SELLER_ID, 2);
        insertInventory(BUYER_ID, 3);
        sourceEntryId = insertEntry(
                SELLER_ID,
                itemId,
                0,
                7,
                "RESERVED_FOR_TRADE"
        );
        insertEntry(BUYER_ID, itemId, 0, 4, "FREE");
        listingId = insertListing();
        insertWalletsAndReservation();
        given(idempotencyKeyStore.acquire(any(), any())).willReturn(true);
    }

    @Nested
    @DisplayName("canonical purchase가 성공하면")
    class SuccessfulPurchase {

        @Test
        @DisplayName("Economy terminal state와 seller removal 및 buyer fulfillment를 함께 commit한다")
        void commitsWholeBusinessEffect() {
            marketplaceService.purchase(
                    BUYER_ID,
                    purchaseCommand("purchase-success-298")
            );

            assertThat(listingStatus()).isEqualTo("SOLD");
            assertThat(reservationState()).isEqualTo("CONSUMED");
            assertThat(holdStatus()).isEqualTo("COMMITTED");
            assertThat(sellerBalance()).isEqualTo(109L);
            assertThat(entryCount(SELLER_ID, itemId)).isZero();
            assertThat(totalQuantity(BUYER_ID, itemId)).isEqualTo(11L);
            assertThat(jdbc.queryForList("""
                    SELECT quantity, rarity, durability, bound, availability,
                           JSON_UNQUOTE(JSON_EXTRACT(inst_attrs, '$.quality')) AS quality
                    FROM inventory_entries
                    WHERE player_id = ? AND item_id = ?
                    ORDER BY slot_index
                    """, BUYER_ID, itemId))
                    .allSatisfy(row -> {
                        assertThat(row.get("rarity")).isEqualTo("RARE");
                        assertThat(row.get("durability")).isEqualTo(6);
                        assertThat(row.get("bound")).isIn(true, (byte) 1);
                        assertThat(row.get("availability")).isEqualTo("FREE");
                        assertThat(row.get("quality")).isEqualTo("kept");
                    });
            assertThat(jdbc.queryForMap("""
                    SELECT item_inst_id, item_id, sale_quantity,
                           buyer_player_id, seller_player_id
                    FROM trades
                    WHERE listing_id = ?
                    """, listingId))
                    .containsEntry("item_inst_id", sourceEntryId)
                    .containsEntry("item_id", itemId)
                    .containsEntry("sale_quantity", 7)
                    .containsEntry("buyer_player_id", BUYER_ID)
                    .containsEntry("seller_player_id", SELLER_ID);
        }
    }

    @Nested
    @DisplayName("buyer Inventory가 가득 찼으면")
    class CapacityFailure {

        @Test
        @DisplayName("구매 전 Economy와 Inventory 상태를 전부 유지한다")
        void rollsBackWholeBusinessEffect() {
            jdbc.update(
                    "DELETE FROM inventory_entries WHERE player_id = ?",
                    BUYER_ID
            );
            jdbc.update(
                    "UPDATE player_inventory SET capacity_slots = 1 WHERE player_id = ?",
                    BUYER_ID
            );
            insertEntry(BUYER_ID, differentItemId(), 0, 1, "FREE");

            assertThatThrownBy(() -> marketplaceService.purchase(
                    BUYER_ID,
                    purchaseCommand("purchase-capacity-298")
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(InventoryError.INVENTORY_FULL)
            );

            assertThat(listingStatus()).isEqualTo("OPEN");
            assertThat(reservationState()).isEqualTo("ACTIVE");
            assertThat(holdStatus()).isEqualTo("OPEN");
            assertThat(sellerBalance()).isEqualTo(10L);
            assertThat(entryCount(SELLER_ID, itemId)).isOne();
            assertThat(jdbc.queryForObject("""
                    SELECT availability
                    FROM inventory_entries
                    WHERE id = ?
                    """, String.class, sourceEntryId))
                    .isEqualTo("RESERVED_FOR_TRADE");
            assertThat(entryCount(BUYER_ID, itemId)).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM trades WHERE listing_id = ?",
                    Long.class,
                    listingId
            )).isZero();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events",
                    Long.class
            )).isZero();
        }
    }

    @Nested
    @DisplayName("V28 Trade snapshot schema를 검증하면")
    class TradeSnapshotSchema {

        @Test
        @DisplayName("legacy null은 허용하고 canonical quantity 제약은 보존한다")
        void validatesMigrationAndJpaContract() {
            assertThat(flyway.info().current().getVersion().getVersion())
                    .isEqualTo("31");
            jdbc.update("""
                    INSERT INTO trades (
                        fee_bps, buyer_player_id, created_at, fee, item_inst_id,
                        listing_id, price, seller_player_id, seller_proceeds,
                        updated_at, currency, fee_currency,
                        seller_proceeds_currency, item_id, sale_quantity
                    ) VALUES (
                        100, ?, CURRENT_TIMESTAMP(6), 1, ?, ?, 100, ?, 99,
                        CURRENT_TIMESTAMP(6), 'GOLD', 'GOLD', 'GOLD', NULL, NULL
                    )
                    """, BUYER_ID, sourceEntryId, listingId + 100, SELLER_ID);

            assertThatThrownBy(() -> jdbc.update("""
                    INSERT INTO trades (
                        fee_bps, buyer_player_id, created_at, fee, item_inst_id,
                        listing_id, price, seller_player_id, seller_proceeds,
                        updated_at, currency, fee_currency,
                        seller_proceeds_currency, item_id, sale_quantity
                    ) VALUES (
                        100, ?, CURRENT_TIMESTAMP(6), 1, ?, ?, 100, ?, 99,
                        CURRENT_TIMESTAMP(6), 'GOLD', 'GOLD', 'GOLD', ?, 0
                    )
                    """, BUYER_ID, sourceEntryId, listingId + 101,
                    SELLER_ID, itemId))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private EconomyCommand.PurchaseListing purchaseCommand(String key) {
        return new EconomyCommand.PurchaseListing(
                listingId,
                RESERVATION_TOKEN,
                key
        );
    }

    private Long insertItem() {
        jdbc.update("""
                INSERT INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    ?, 'Issue 298 marketplace item', 'QUEST', 'ETC',
                    'RARE', JSON_OBJECT(), TRUE, 10, 10,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, ITEM_CODE);
        return jdbc.queryForObject(
                "SELECT id FROM items WHERE code = ?",
                Long.class,
                ITEM_CODE
        );
    }

    private Long differentItemId() {
        jdbc.update("""
                INSERT INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    'IT_298_BLOCKER', 'Issue 298 blocker', 'QUEST', 'ETC',
                    'RARE', JSON_OBJECT(), FALSE, 1, 10,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                ) ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP(6)
                """);
        return jdbc.queryForObject(
                "SELECT id FROM items WHERE code = 'IT_298_BLOCKER'",
                Long.class
        );
    }

    private void insertInventory(long playerId, int capacity) {
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version,
                    created_at, updated_at
                ) VALUES (?, ?, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, capacity);
    }

    private Long insertEntry(
            long playerId,
            long entryItemId,
            int slot,
            int quantity,
            String availability
    ) {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity, availability
                ) VALUES (
                    TRUE, 6, ?, ?, CURRENT_TIMESTAMP(6), ?, ?,
                    CURRENT_TIMESTAMP(6), JSON_OBJECT('quality', 'kept'),
                    'RARE', ?
                )
                """, quantity, slot, entryItemId, playerId, availability);
        return jdbc.queryForObject("""
                SELECT id
                FROM inventory_entries
                WHERE player_id = ? AND slot_index = ?
                """, Long.class, playerId, slot);
    }

    private Long insertListing() {
        jdbc.update("""
                INSERT INTO listings (
                    active_flag, created_at, item_id, sale_quantity,
                    item_inst_id, price, seller_player_id, updated_at,
                    version, currency, status
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6), ?, 7, ?, 100, ?,
                    CURRENT_TIMESTAMP(6), 0, 'GOLD', 'OPEN'
                )
                """, itemId, sourceEntryId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM listings WHERE item_inst_id = ?",
                Long.class,
                sourceEntryId
        );
    }

    private void insertWalletsAndReservation() {
        insertWallet(BUYER_ID, 100L);
        insertWallet(SELLER_ID, 10L);
        Long buyerWalletId = walletId(BUYER_ID);
        jdbc.update("""
                INSERT INTO wallet_holds (
                    amount, created_at, expires_at, updated_at, wallet_id,
                    hold_id, reason, currency, status
                ) VALUES (
                    100, CURRENT_TIMESTAMP(6),
                    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR),
                    CURRENT_TIMESTAMP(6), ?, ?, 'listing-reserve',
                    'GOLD', 'OPEN'
                )
                """, buyerWalletId, HOLD_ID);
        jdbc.update("""
                INSERT INTO listing_reservations (
                    active_flag, created_at, expires_at, buyer_player_id,
                    listing_id, updated_at, version, reservation_token,
                    wallet_hold_id, state
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6),
                    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR),
                    ?, ?, CURRENT_TIMESTAMP(6), 0, ?, ?, 'ACTIVE'
                )
                """, BUYER_ID, listingId, RESERVATION_TOKEN, HOLD_ID);
    }

    private void insertWallet(long ownerId, long balance) {
        jdbc.update("""
                INSERT INTO wallets (
                    created_at, owner_id, updated_at, version
                ) VALUES (
                    CURRENT_TIMESTAMP(6), ?, CURRENT_TIMESTAMP(6), 0
                )
                """, ownerId);
        jdbc.update("""
                INSERT INTO wallet_balances (
                    amount, created_at, updated_at, wallet_id, currency
                ) VALUES (
                    ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, 'GOLD'
                )
                """, balance, walletId(ownerId));
    }

    private Long walletId(long ownerId) {
        return jdbc.queryForObject(
                "SELECT id FROM wallets WHERE owner_id = ?",
                Long.class,
                ownerId
        );
    }

    private String listingStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM listings WHERE id = ?",
                String.class,
                listingId
        );
    }

    private String reservationState() {
        return jdbc.queryForObject("""
                SELECT state FROM listing_reservations WHERE listing_id = ?
                """, String.class, listingId);
    }

    private String holdStatus() {
        return jdbc.queryForObject(
                "SELECT status FROM wallet_holds WHERE hold_id = ?",
                String.class,
                HOLD_ID
        );
    }

    private long sellerBalance() {
        return jdbc.queryForObject("""
                SELECT balance.amount
                FROM wallet_balances balance
                JOIN wallets wallet ON wallet.id = balance.wallet_id
                WHERE wallet.owner_id = ? AND balance.currency = 'GOLD'
                """, Long.class, SELLER_ID);
    }

    private long entryCount(long playerId, long entryItemId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_entries
                WHERE player_id = ? AND item_id = ?
                """, Long.class, playerId, entryItemId);
    }

    private long totalQuantity(long playerId, long entryItemId) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0) FROM inventory_entries
                WHERE player_id = ? AND item_id = ?
                """, Long.class, playerId, entryItemId);
    }
}
