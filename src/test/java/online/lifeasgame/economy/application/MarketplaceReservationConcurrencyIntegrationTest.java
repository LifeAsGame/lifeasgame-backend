package online.lifeasgame.economy.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.api.player.mapper.EconomyWebMapper;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.infra.JpaListingReservationRepository;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.MySQLContainer;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Marketplace reservation MySQL 조회·동시성")
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

    @Autowired
    private ListingReservationReader reservationReader;
    @Autowired
    private WalletQueryService walletQueryService;
    @Autowired
    private JpaListingReservationRepository reservationRepository;
    @Autowired
    private EconomyFacade facade;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;
    @MockitoSpyBean
    private DomainEventPublisher eventPublisher;

    private Long listingId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM marketplace_purchase_receipts");
        jdbc.update("DELETE FROM trades");
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
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(SELLER_ID);
    }

    @Nested
    @DisplayName("effective Listing status를 조회하면")
    class EffectiveStatus {

        @Test
        @DisplayName("실제 reserve 이후 public·seller는 RESERVED이며 조회가 영속 상태를 변경하지 않는다")
        void readsEffectiveReservedStatus() throws Exception {
            assertStatuses("OPEN");
            var reservation = reserve();
            assertBuyerWallet(50, 50);
            var before = snapshot();
            clearInvocations(eventPublisher);

            assertStatuses("RESERVED");
            assertThat(marketplaceService.listReservations(BUYER_ID).reservations()).singleElement()
                    .satisfies(row -> assertThat(row.listingId()).isEqualTo(listingId));
            assertThat(marketplaceService.listReservations(BUYER_ID + 1).reservations()).isEmpty();
            given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(BUYER_ID);
            assertThat(facade.myListings().listings()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id = ?", String.class, listingId))
                    .isEqualTo("OPEN");
            var publicJson = objectMapper.valueToTree(EconomyWebMapper.toListings(marketplaceService.listOpen()));
            var sellerJson = objectMapper.valueToTree(EconomyWebMapper.toPlayerListings(marketplaceService.listBySeller(SELLER_ID)));
            for (var json : List.of(publicJson, sellerJson)) {
                assertThat(json.get("listings").get(0).properties()).extracting(Map.Entry::getKey)
                        .containsExactlyInAnyOrder("id", "itemId", "sellerId", "price", "currency", "status");
                assertThat(json.toString()).doesNotContain(reservation.holdId(), reservation.reservationToken());
            }
            assertThat(snapshot()).isEqualTo(before);
            verifyNoInteractions(eventPublisher);

            assertError(() -> reserve(), EconomyError.LISTING_RESERVED_OTHER);
            assertError(() -> marketplaceService.cancel(SELLER_ID, cancelCommand()), EconomyError.LISTING_ACTIVE_RESERVATION);
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("확정 후 public에서 제외되고 seller는 SOLD이며 동일 구매 재시도는 기존 거래를 반환한다")
        void readsSoldAfterPurchase() {
            jdbc.update("""
                    INSERT INTO player_inventory (player_id, capacity_slots, version, created_at, updated_at)
                    VALUES (?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                    """, BUYER_ID);
            var reservation = reserve();
            assertStatuses("RESERVED");
            var command = new EconomyCommand.PurchaseListing(listingId, reservation.reservationToken(), "read-purchase-350");
            var trade = marketplaceService.purchase(BUYER_ID, command);
            assertBuyerWallet(50, 0);
            var before = snapshot();

            assertThat(marketplaceService.listOpen().listings()).isEmpty();
            assertSellerStatus("SOLD");
            assertThat(marketplaceService.listReservations(BUYER_ID).reservations()).isEmpty();
            assertThat(snapshot()).isEqualTo(before);
            assertThat(marketplaceService.purchase(BUYER_ID, command)).isEqualTo(trade);
            assertThat(jdbc.queryForObject("SELECT quantity FROM inventory_entries WHERE player_id = ?", Integer.class, BUYER_ID))
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("판매자 취소 후 public에서 제외되고 seller는 CANCELED이며 타인 취소는 거절한다")
        void readsCanceledAfterSellerCancel() {
            assertStatuses("OPEN");
            assertError(() -> marketplaceService.cancel(BUYER_ID, cancelCommand()), EconomyError.LISTING_NOT_AVAILABLE);
            marketplaceService.cancel(SELLER_ID, cancelCommand());
            var before = snapshot();

            assertThat(marketplaceService.listOpen().listings()).isEmpty();
            assertSellerStatus("CANCELED");
            assertThat(snapshot()).isEqualTo(before);
            assertError(() -> reserve(), EconomyError.LISTING_NOT_AVAILABLE);
        }

        @ParameterizedTest
        @ValueSource(longs = {-1, 0, 1})
        @DisplayName("만료 직전·동일 시각·직후에도 ACTIVE는 RESERVED이며 기존 strict expiry 경계를 유지한다")
        void preservesExpiryBoundary(long microsecondsAfterExpiry) {
            var reservation = reserve();
            var stored = reservationRepository.findAll().getFirst();
            Instant cutoff = stored.getExpiresAt().plusNanos(microsecondsAfterExpiry * 1000);
            var before = snapshot();

            assertStatuses("RESERVED");
            assertThat(reservationReader.findActiveListingIdsExpiringBefore(cutoff))
                    .containsExactlyElementsOf(microsecondsAfterExpiry > 0 ? List.of(listingId) : List.of());
            if (microsecondsAfterExpiry > 0) {
                assertError(() -> stored.validatePurchase(BUYER_ID, reservation.reservationToken(), cutoff),
                        EconomyError.LISTING_RESERVATION_EXPIRED);
            } else {
                stored.validatePurchase(BUYER_ID, reservation.reservationToken(), cutoff);
            }
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("시간만 지난 예약은 계속 RESERVED이며 expiry 정리 commit 후에만 OPEN과 재예약·취소를 허용한다")
        void reopensOnlyAfterCleanup() {
            var reservation = reserve();
            Timestamp expired = Timestamp.valueOf("2000-01-01 00:00:00");
            jdbc.update("UPDATE listing_reservations SET expires_at = ? WHERE listing_id = ?", expired, listingId);
            jdbc.update("UPDATE wallet_holds SET expires_at = ? WHERE hold_id = ?", expired, reservation.holdId());
            assertThat(reservationRepository.findAll().getFirst().isExpiredAt(Instant.now())).isTrue();
            var before = snapshot();
            clearInvocations(eventPublisher);

            assertStatuses("RESERVED");
            assertBuyerWallet(50, 50);
            assertThat(marketplaceService.listReservations(BUYER_ID).reservations()).hasSize(1);
            assertError(() -> reserve(), EconomyError.LISTING_RESERVED_OTHER);
            assertError(() -> marketplaceService.cancel(SELLER_ID, cancelCommand()), EconomyError.LISTING_ACTIVE_RESERVATION);
            assertError(() -> marketplaceService.purchase(BUYER_ID,
                    new EconomyCommand.PurchaseListing(listingId, reservation.reservationToken(), "expired-350")),
                    EconomyError.LISTING_RESERVATION_EXPIRED);
            assertThat(snapshot()).isEqualTo(before);
            verifyNoInteractions(eventPublisher);

            marketplaceService.expireReservations();
            var cleaned = snapshot();
            assertStatuses("OPEN");
            assertBuyerWallet(100, 0);
            assertThat(marketplaceService.listReservations(BUYER_ID).reservations()).isEmpty();
            assertThat(jdbc.queryForObject("SELECT status FROM wallet_holds WHERE hold_id = ?", String.class, reservation.holdId()))
                    .isEqualTo("EXPIRED");
            assertThat(jdbc.queryForObject("SELECT availability FROM inventory_entries", String.class)).isEqualTo("LISTED");
            assertThat(snapshot()).isEqualTo(cleaned);
            marketplaceService.expireReservations();
            assertThat(snapshot()).isEqualTo(cleaned);
            var renewed = reserve();
            assertThat(renewed.reservationToken()).isNotEqualTo(reservation.reservationToken());
            assertStatuses("RESERVED");
            jdbc.update("UPDATE listing_reservations SET expires_at = ? WHERE state = 'ACTIVE'", expired);
            jdbc.update("UPDATE wallet_holds SET expires_at = ? WHERE status = 'OPEN'", expired);
            marketplaceService.expireReservations();
            marketplaceService.cancel(SELLER_ID, cancelCommand());
            assertSellerStatus("CANCELED");
        }

        @Test
        @DisplayName("여러 매물도 예약 ID 조회는 목록당 한 번이며 비어 있거나 terminal뿐이면 추가 조회하지 않는다")
        void batchesOnlyNecessaryReservationReads() {
            reserve();
            Long itemId = jdbc.queryForObject("SELECT item_id FROM listings WHERE id = ?", Long.class, listingId);
            Long second = insertListing(itemId, insertEntry(itemId, 1));
            Long third = insertListing(itemId, insertEntry(itemId, 2));
            var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            assertThat(marketplaceService.listOpen().listings()).extracting(EconomyResult.ListingSummary::status)
                    .containsExactlyInAnyOrder("RESERVED", "OPEN", "OPEN");
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
            statistics.clear();
            assertThat(marketplaceService.listBySeller(SELLER_ID).listings()).hasSize(3);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
            statistics.clear();
            assertThat(marketplaceService.listBySeller(BUYER_ID).listings()).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            marketplaceService.cancel(SELLER_ID, new EconomyCommand.CancelListing(second));
            marketplaceService.cancel(SELLER_ID, new EconomyCommand.CancelListing(third));
            Timestamp expired = Timestamp.valueOf("2000-01-01 00:00:00");
            jdbc.update("UPDATE listing_reservations SET expires_at = ? WHERE state = 'ACTIVE'", expired);
            jdbc.update("UPDATE wallet_holds SET expires_at = ? WHERE status = 'OPEN'", expired);
            marketplaceService.expireReservations();
            marketplaceService.cancel(SELLER_ID, cancelCommand());
            statistics.clear();
            assertThat(marketplaceService.listBySeller(SELLER_ID).listings()).extracting(EconomyResult.ListingSummary::status)
                    .containsOnly("CANCELED");
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
            statistics.clear();
            assertThat(marketplaceService.listOpen().listings()).isEmpty();
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    private void assertBuyerWallet(long available, long held) {
        var result = walletQueryService.wallet(BUYER_ID);
        assertThat(result.amount()).isEqualTo(available);
        assertThat(result.balances()).containsExactly(
                new EconomyResult.CurrencyBalance(online.lifeasgame.economy.domain.Currency.GOLD, available, held),
                new EconomyResult.CurrencyBalance(online.lifeasgame.economy.domain.Currency.GEM, 0, 0));
    }

    private EconomyResult.Reservation reserve() {
        return marketplaceService.reserve(BUYER_ID, new EconomyCommand.ReserveListing(listingId, 3600));
    }

    private EconomyCommand.CancelListing cancelCommand() { return new EconomyCommand.CancelListing(listingId); }

    private void assertStatuses(String expected) {
        assertThat(marketplaceService.listOpen().listings()).singleElement()
                .extracting(EconomyResult.ListingSummary::status).isEqualTo(expected);
        assertSellerStatus(expected);
    }

    private void assertSellerStatus(String expected) {
        assertThat(facade.myListings().listings()).singleElement()
                .extracting(EconomyResult.ListingSummary::status).isEqualTo(expected);
    }

    private void assertError(Runnable action, EconomyError expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(DomainException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(expected));
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : List.of("listings", "listing_reservations", "wallets", "wallet_balances", "wallet_holds",
                "inventory_entries", "trades", "marketplace_purchase_receipts", "outbox_events")) {
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id"));
        }
        return result;
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
        return insertEntry(itemId, 0);
    }

    private Long insertEntry(Long itemId, int slot) {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity, availability
                ) VALUES (
                    FALSE, NULL, 4, ?, CURRENT_TIMESTAMP(6), ?, ?,
                    CURRENT_TIMESTAMP(6), JSON_OBJECT(), 'COMMON', 'LISTED'
                )
                """, slot, itemId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM inventory_entries WHERE player_id = ? AND slot_index = ?",
                Long.class,
                SELLER_ID, slot
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
