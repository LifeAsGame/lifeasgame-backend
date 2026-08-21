package online.lifeasgame.economy.application;

import online.lifeasgame.economy.application.command.EconomyCommand;
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
import org.testcontainers.containers.MySQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Marketplace reservation MySQL 동시성")
class MarketplaceReservationConcurrencyIntegrationTest {

    private static final long SELLER_ID = 296001L;
    private static final long BUYER_ID = 296002L;
    private static final String ITEM_CODE = "IT_296_MARKETPLACE";

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_marketplace_296")
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
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private MarketplaceService marketplaceService;
    @Autowired
    private JdbcTemplate jdbc;

    private Long listingId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM listing_reservations");
        jdbc.update("DELETE FROM wallet_holds");
        jdbc.update("DELETE FROM wallet_balances");
        jdbc.update("DELETE FROM wallets");
        jdbc.update("DELETE FROM listings");
        jdbc.update("DELETE FROM inventory_entries");
        jdbc.update("DELETE FROM player_inventory");
        jdbc.update("DELETE FROM items WHERE code = ?", ITEM_CODE);

        Long itemId = insertItem();
        insertInventory();
        Long entryId = insertEntry(itemId);
        listingId = insertListing(itemId, entryId);
        insertBuyerWallet();
    }

    @Nested
    @DisplayName("reserve와 seller cancel이 같은 Listing에서 경쟁하면")
    class ReserveCancelRace {

        @Test
        @DisplayName("Listing lock으로 정확히 하나만 성공하고 연관 상태가 함께 확정된다")
        void oneWinner() throws Exception {
            List<Throwable> outcomes = race(
                    () -> marketplaceService.reserve(
                            BUYER_ID,
                            new EconomyCommand.ReserveListing(listingId, 3600)
                    ),
                    () -> marketplaceService.cancel(
                            SELLER_ID,
                            new EconomyCommand.CancelListing(listingId)
                    )
            );

            assertThat(outcomes.stream().filter(Objects::isNull).count()).isEqualTo(1);
            long activeReservations = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM listing_reservations
                    WHERE listing_id = ? AND state = 'ACTIVE'
                    """, Long.class, listingId);
            String listingStatus = jdbc.queryForObject(
                    "SELECT status FROM listings WHERE id = ?",
                    String.class,
                    listingId
            );
            String availability = jdbc.queryForObject("""
                    SELECT entry.availability
                    FROM inventory_entries entry
                    JOIN listings listing ON listing.item_inst_id = entry.id
                    WHERE listing.id = ?
                    """, String.class, listingId);
            long openHolds = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM wallet_holds WHERE status = 'OPEN'",
                    Long.class
            );

            if (activeReservations == 1) {
                assertThat(listingStatus).isEqualTo("OPEN");
                assertThat(availability).isEqualTo("RESERVED_FOR_TRADE");
                assertThat(openHolds).isEqualTo(1);
            } else {
                assertThat(activeReservations).isZero();
                assertThat(listingStatus).isEqualTo("CANCELED");
                assertThat(availability).isEqualTo("FREE");
                assertThat(openHolds).isZero();
            }
        }
    }

    @Nested
    @DisplayName("동일 Listing에 ACTIVE 예약을 중복 저장하면")
    class ActiveReservationUniqueness {

        @Test
        @DisplayName("DB unique constraint가 두 번째 ACTIVE 예약을 거절한다")
        void rejectsDuplicateActiveReservation() {
            insertReservation("token-296-a", "hold-296-a");

            assertThatThrownBy(() -> insertReservation("token-296-b", "hold-296-b"))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private List<Throwable> race(Runnable first, Runnable second) throws Exception {
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
                    ?, 'Issue 296 marketplace item', 'QUEST', 'ETC',
                    'COMMON', JSON_OBJECT(), TRUE, 10, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, ITEM_CODE);
        return jdbc.queryForObject("SELECT id FROM items WHERE code = ?", Long.class, ITEM_CODE);
    }

    private void insertInventory() {
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, SELLER_ID);
    }

    private Long insertEntry(Long itemId) {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity, availability
                ) VALUES (
                    FALSE, NULL, 4, 0, CURRENT_TIMESTAMP(6), ?, ?,
                    CURRENT_TIMESTAMP(6), JSON_OBJECT(), 'COMMON', 'LISTED'
                )
                """, itemId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM inventory_entries WHERE player_id = ?",
                Long.class,
                SELLER_ID
        );
    }

    private Long insertListing(Long itemId, Long entryId) {
        jdbc.update("""
                INSERT INTO listings (
                    active_flag, created_at, id, item_id, sale_quantity,
                    item_inst_id, price, seller_player_id, updated_at,
                    version, currency, status
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6), NULL, ?, 4, ?, 50, ?,
                    CURRENT_TIMESTAMP(6), 0, 'GOLD', 'OPEN'
                )
                """, itemId, entryId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM listings WHERE item_inst_id = ?",
                Long.class,
                entryId
        );
    }

    private void insertBuyerWallet() {
        jdbc.update("""
                INSERT INTO wallets (created_at, owner_id, updated_at, version)
                VALUES (CURRENT_TIMESTAMP(6), ?, CURRENT_TIMESTAMP(6), 0)
                """, BUYER_ID);
        Long walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE owner_id = ?",
                Long.class,
                BUYER_ID
        );
        jdbc.update("""
                INSERT INTO wallet_balances (
                    amount, created_at, updated_at, wallet_id, currency
                ) VALUES (100, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, 'GOLD')
                """, walletId);
    }

    private void insertReservation(String token, String holdId) {
        jdbc.update("""
                INSERT INTO listing_reservations (
                    active_flag, created_at, expires_at, buyer_player_id,
                    listing_id, updated_at, version, reservation_token,
                    wallet_hold_id, state
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6), DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR),
                    ?, ?, CURRENT_TIMESTAMP(6), 0, ?, ?, 'ACTIVE'
                )
                """, BUYER_ID, listingId, token, holdId);
    }
}
