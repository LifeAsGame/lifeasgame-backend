package online.lifeasgame.economy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import online.lifeasgame.LifeasgameApplication;
import online.lifeasgame.economy.application.port.ShopReservationLimiter;
import online.lifeasgame.inventory.application.InventoryAvailabilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = LifeasgameApplication.class,
        properties = "lifeasgame.economy.reservation-expiry-ms=3600000")
@ActiveProfiles({"test", "migration-test"})
@Testcontainers
@DisplayName("예약 만료 작업의 실제 트랜잭션 경계")
class EconomyReservationFailureIsolationIntegrationTest {

    private static final long SELLER = 920001L;
    private static final long MARKET_BUYER = 920002L;
    private static final long SHOP_BUYER = 920003L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("reservation_failure_isolation")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired EconomyReservationScheduler scheduler;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean InventoryAvailabilityService availability;
    @MockitoBean ShopReservationLimiter shopLimiter;

    @Test
    @DisplayName("Marketplace 영속 변경이 롤백되어도 Shop 예약과 hold는 별도 커밋한다")
    void commitsShopAfterMarketplaceRollback() {
        long entryId = seedInventoryAndListing();
        seedWallet(MARKET_BUYER, "market-hold");
        seedWallet(SHOP_BUYER, "shop-hold");
        jdbc.update("""
                INSERT INTO listing_reservations (active_flag, created_at, expires_at,
                    buyer_player_id, listing_id, updated_at, version, reservation_token,
                    wallet_hold_id, state)
                SELECT 1, NOW(6), DATE_SUB(NOW(6), INTERVAL 30 SECOND), ?, id,
                    NOW(6), 0, 'market-reservation', 'market-hold', 'ACTIVE'
                FROM listings
                """, MARKET_BUYER);
        jdbc.update("""
                INSERT INTO shop_purchases (quantity, created_at, player_id,
                    reservation_expires_at, shop_item_id, total_price, updated_at,
                    reservation_token, wallet_hold_id, currency, status)
                SELECT 1, NOW(6), ?, DATE_SUB(NOW(6), INTERVAL 30 SECOND),
                    id, 10, NOW(6), 'shop-reservation', 'shop-hold', 'GOLD', 'RESERVED'
                FROM shop_items
                """, SHOP_BUYER);

        doThrow(new IllegalStateException("inventory release failed"))
                .when(availability).releaseTradeReservation(SELLER, entryId);

        scheduler.expireReservations();

        assertThat(value("SELECT state FROM listing_reservations"))
                .isEqualTo("ACTIVE");
        assertThat(value("SELECT status FROM wallet_holds WHERE hold_id = 'market-hold'"))
                .isEqualTo("OPEN");
        assertThat(balance(MARKET_BUYER)).isEqualTo(90);
        assertThat(value("SELECT status FROM shop_purchases"))
                .isEqualTo("EXPIRED");
        assertThat(value("SELECT status FROM wallet_holds WHERE hold_id = 'shop-hold'"))
                .isEqualTo("EXPIRED");
        assertThat(balance(SHOP_BUYER)).isEqualTo(100);
        assertThat(eventCount("SHOP_RESERVATION_EXPIRED"))
                .isEqualTo(1);
        assertThat(eventCount("LISTING_RESERVATION_EXPIRED"))
                .isZero();
    }

    private long seedInventoryAndListing() {
        jdbc.update("""
                INSERT INTO items (code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, created_at, updated_at)
                VALUES ('EXPIRY_ISOLATION_ITEM', 'Expiry isolation item', 'QUEST', 'ETC',
                    'COMMON', JSON_OBJECT(), TRUE, 10, NOW(6), NOW(6))
                """);
        long itemId = jdbc.queryForObject(
                "SELECT id FROM items WHERE code = 'EXPIRY_ISOLATION_ITEM'", Long.class);
        jdbc.update("""
                INSERT INTO player_inventory (player_id, capacity_slots, version,
                    created_at, updated_at)
                VALUES (?, 10, 0, NOW(6), NOW(6))
                """, SELLER);
        jdbc.update("""
                INSERT INTO inventory_entries (bound, quantity, slot_index, item_id,
                    player_id, inst_attrs, rarity, availability, created_at, updated_at)
                VALUES (FALSE, 1, 0, ?, ?, JSON_OBJECT(), 'COMMON',
                    'RESERVED_FOR_TRADE', NOW(6), NOW(6))
                """, itemId, SELLER);
        long entryId = jdbc.queryForObject(
                "SELECT id FROM inventory_entries WHERE player_id = ?", Long.class, SELLER);
        jdbc.update("""
                INSERT INTO listings (active_flag, item_id, sale_quantity, item_inst_id,
                    price, seller_player_id, version, currency, status, created_at, updated_at)
                VALUES (1, ?, 1, ?, 10, ?, 0, 'GOLD', 'OPEN', NOW(6), NOW(6))
                """, itemId, entryId, SELLER);
        jdbc.update("""
                INSERT INTO shop_items (available, item_id, price, currency,
                    created_at, updated_at, version)
                VALUES (TRUE, ?, 10, 'GOLD', NOW(6), NOW(6), 0)
                """, itemId);
        return entryId;
    }

    private void seedWallet(long playerId, String holdId) {
        jdbc.update("""
                INSERT INTO wallets (owner_id, version, created_at, updated_at)
                VALUES (?, 0, NOW(6), NOW(6))
                """, playerId);
        long walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE owner_id = ?", Long.class, playerId);
        jdbc.update("""
                INSERT INTO wallet_balances (wallet_id, amount, currency, created_at, updated_at)
                VALUES (?, 90, 'GOLD', NOW(6), NOW(6))
                """, walletId);
        jdbc.update("""
                INSERT INTO wallet_holds (wallet_id, hold_id, amount, currency, status,
                    expires_at, created_at, updated_at)
                VALUES (?, ?, 10, 'GOLD', 'OPEN',
                    DATE_SUB(NOW(6), INTERVAL 30 SECOND), NOW(6), NOW(6))
                """, walletId, holdId);
    }

    private String value(String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private long balance(long playerId) {
        return jdbc.queryForObject("""
                SELECT balance.amount FROM wallet_balances balance
                JOIN wallets wallet ON wallet.id = balance.wallet_id
                WHERE wallet.owner_id = ? AND balance.currency = 'GOLD'
                """, Long.class, playerId);
    }

    private long eventCount(String type) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM outbox_events
                WHERE event_type = 'economy.event.v1'
                AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.type')) = ?
                """, Long.class, type);
    }
}
