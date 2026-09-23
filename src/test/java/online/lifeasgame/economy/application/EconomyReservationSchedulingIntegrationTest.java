package online.lifeasgame.economy.application;

import online.lifeasgame.LifeasgameApplication;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = LifeasgameApplication.class,
        properties = "lifeasgame.economy.reservation-expiry-ms=100")
@ActiveProfiles({"test", "migration-test"})
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("운영 scheduling 설정으로 Marketplace 예약을 자연 만료시키면")
class EconomyReservationSchedulingIntegrationTest {

    private static final long SELLER = 910001L;
    private static final long BUYER = 910002L;
    private static final long WAITING_BUYER = 910003L;
    private static final long PAID_BUYER = 910004L;

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("reservation_scheduling")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired MarketplaceService marketplace;
    @Autowired WalletQueryService wallets;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("hold와 Inventory 예약을 한 번만 해제하고 재예약·취소를 허용하며 미만료·구매 완료 거래는 보존한다")
    void expiresThroughRegisteredJob() {
        seedAssets();
        long expiring = listing(0);
        long waiting = listing(1);
        long purchased = listing(2);
        var waitingReservation = reserve(WAITING_BUYER, waiting, 3600);
        var paidReservation = reserve(PAID_BUYER, purchased, 5);
        marketplace.purchase(PAID_BUYER, new EconomyCommand.PurchaseListing(
                purchased, paidReservation.reservationToken(), "scheduled-purchase"));
        var reservation = reserve(BUYER, expiring, 2);
        assertWallet(BUYER, 90, 10);
        assertListing(expiring, "RESERVED");
        assertAvailability(0, "RESERVED_FOR_TRADE");

        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            assertReleased(expiring, reservation);
            assertThat(expiryEvents()).isEqualTo(1);
        });

        // Observe further scheduled cycles, including after the consumed reservation's original TTL.
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            assertThat(Instant.now()).isAfter(paidReservation.expiresAt());
        });
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
                    assertReleased(expiring, reservation);
                    assertThat(expiryEvents()).isEqualTo(1);
                    assertListing(waiting, "RESERVED");
                    assertReservation(waitingReservation, "ACTIVE", "OPEN");
                    assertWallet(WAITING_BUYER, 90, 10);
                    assertAvailability(1, "RESERVED_FOR_TRADE");
                    assertListing(purchased, "SOLD");
                    assertReservation(paidReservation, "CONSUMED", "COMMITTED");
                    assertWallet(PAID_BUYER, 90, 0);
                    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trades", Long.class)).isEqualTo(1);
                    assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_entries WHERE player_id = ?",
                            Integer.class, PAID_BUYER)).isEqualTo(1);
                });

        var renewed = reserve(BUYER, expiring, 1);
        assertThat(renewed.reservationToken()).isNotEqualTo(reservation.reservationToken());
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            assertReleased(expiring, renewed);
            assertThat(expiryEvents()).isEqualTo(2);
        });
        marketplace.cancel(SELLER, new EconomyCommand.CancelListing(expiring));
        assertListing(expiring, "CANCELED");
        assertAvailability(0, "FREE");
        assertWallet(BUYER, 100, 0);
    }

    private EconomyResult.Reservation reserve(long buyer, long listingId, int ttl) {
        return marketplace.reserve(buyer, new EconomyCommand.ReserveListing(listingId, ttl));
    }

    private void assertReleased(long listingId, EconomyResult.Reservation reservation) {
        assertReservation(reservation, "EXPIRED", "EXPIRED");
        assertListing(listingId, "OPEN");
        assertThat(marketplace.listOpen().listings()).anySatisfy(row -> {
            assertThat(row.id()).isEqualTo(listingId);
            assertThat(row.status()).isEqualTo("OPEN");
        });
        assertWallet(BUYER, 100, 0);
        assertThat(marketplace.listReservations(BUYER).reservations()).isEmpty();
        assertAvailability(0, "LISTED");
    }

    private void assertReservation(EconomyResult.Reservation reservation, String state, String holdStatus) {
        assertThat(jdbc.queryForObject("SELECT state FROM listing_reservations WHERE reservation_token = ?",
                String.class, reservation.reservationToken())).isEqualTo(state);
        assertThat(jdbc.queryForObject("SELECT status FROM wallet_holds WHERE hold_id = ?",
                String.class, reservation.holdId())).isEqualTo(holdStatus);
    }

    private void assertWallet(long player, long available, long held) {
        assertThat(wallets.wallet(player).balances())
                .contains(new EconomyResult.CurrencyBalance(Currency.GOLD, available, held));
    }

    private void assertListing(long id, String status) {
        assertThat(marketplace.listBySeller(SELLER).listings()).anySatisfy(row -> {
            assertThat(row.id()).isEqualTo(id);
            assertThat(row.status()).isEqualTo(status);
        });
    }

    private void assertAvailability(int slot, String availability) {
        assertThat(jdbc.queryForObject("SELECT availability FROM inventory_entries WHERE player_id = ? AND slot_index = ?",
                String.class, SELLER, slot)).isEqualTo(availability);
    }

    private long expiryEvents() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM outbox_events WHERE event_type = 'economy.event.v1'
                AND JSON_UNQUOTE(JSON_EXTRACT(payload, '$.type')) = 'LISTING_RESERVATION_EXPIRED'
                """, Long.class);
    }

    private long listing(int slot) {
        return jdbc.queryForObject("""
                SELECT listing.id FROM listings listing JOIN inventory_entries entry ON listing.item_inst_id = entry.id
                WHERE entry.player_id = ? AND entry.slot_index = ?
                """, Long.class, SELLER, slot);
    }

    private void seedAssets() {
        jdbc.update("""
                INSERT INTO items (code, name, category, type, rarity, base_attrs, stackable, max_stack, created_at, updated_at)
                VALUES ('SCHEDULING_ITEM', 'Scheduling item', 'QUEST', 'ETC', 'COMMON', JSON_OBJECT(), TRUE, 10, NOW(6), NOW(6))
                """);
        long itemId = jdbc.queryForObject("SELECT id FROM items WHERE code = 'SCHEDULING_ITEM'", Long.class);
        for (long player : new long[]{SELLER, BUYER, WAITING_BUYER, PAID_BUYER}) {
            jdbc.update("""
                    INSERT INTO player_inventory (player_id, capacity_slots, version, created_at, updated_at)
                    VALUES (?, 10, 0, NOW(6), NOW(6))
                    """, player);
            jdbc.update("INSERT INTO wallets (owner_id, version, created_at, updated_at) VALUES (?, 0, NOW(6), NOW(6))", player);
            jdbc.update("""
                    INSERT INTO wallet_balances (wallet_id, amount, currency, created_at, updated_at)
                    SELECT id, 100, 'GOLD', NOW(6), NOW(6) FROM wallets WHERE owner_id = ?
                    """, player);
        }
        for (int slot = 0; slot < 3; slot++) {
            jdbc.update("""
                    INSERT INTO inventory_entries (bound, quantity, slot_index, item_id, player_id, inst_attrs, rarity,
                        availability, created_at, updated_at)
                    VALUES (FALSE, 1, ?, ?, ?, JSON_OBJECT(), 'COMMON', 'LISTED', NOW(6), NOW(6))
                    """, slot, itemId, SELLER);
            jdbc.update("""
                    INSERT INTO listings (active_flag, item_id, sale_quantity, item_inst_id, price, seller_player_id,
                        version, currency, status, created_at, updated_at)
                    SELECT 1, item_id, quantity, id, 10, player_id, 0, 'GOLD', 'OPEN', NOW(6), NOW(6)
                    FROM inventory_entries WHERE player_id = ? AND slot_index = ?
                    """, SELLER, slot);
        }
    }
}
