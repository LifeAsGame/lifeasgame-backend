package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V27 canonical ListingReservation migration")
class ListingReservationFlywayTest {

    private static final long SELLER_ID = 296101L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_listing_reservation")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    private JdbcTemplate jdbc;
    private Long listingId;
    private Long entryId;

    @BeforeEach
    void migrateThroughV26() {
        Flyway throughV26 = flyway(MigrationVersion.fromVersion("26"));
        throughV26.clean();
        throughV26.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
        Long itemId = insertItem();
        insertInventory();
        entryId = insertEntry(itemId);
        insertBuyerWalletAndHold();
        listingId = insertLegacyReservation(itemId, entryId);
    }

    @Test
    @DisplayName("legacy 수량은 추정하지 않고 완전한 예약만 분리하여 Inventory 상태와 제약을 맞춘다")
    void migratesCanonicalReservationSchema() {
        var result = flyway(MigrationVersion.fromVersion("27")).migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT sale_quantity, status, reserved_by, reservation_token,
                       reservation_expires_at, reserved_hold_id
                FROM listings WHERE id = ?
                """, listingId))
                .containsEntry("sale_quantity", null)
                .containsEntry("status", "OPEN")
                .containsEntry("reserved_by", null)
                .containsEntry("reservation_token", null)
                .containsEntry("reservation_expires_at", null)
                .containsEntry("reserved_hold_id", null);
        assertThat(jdbc.queryForMap("""
                SELECT buyer_player_id, reservation_token, wallet_hold_id,
                       state, active_flag
                FROM listing_reservations WHERE listing_id = ?
                """, listingId))
                .containsEntry("buyer_player_id", 296102L)
                .containsEntry("reservation_token", "legacy-token-296")
                .containsEntry("wallet_hold_id", "legacy-hold-296")
                .containsEntry("state", "ACTIVE")
                .containsEntry("active_flag", 1);
        assertThat(jdbc.queryForObject(
                "SELECT availability FROM inventory_entries WHERE id = ?",
                String.class,
                entryId
        )).isEqualTo("RESERVED_FOR_TRADE");

        assertThatThrownBy(() -> insertActiveReservation("second-token-296"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE listings SET sale_quantity = 0 WHERE id = ?",
                listingId
        )).isInstanceOf(DataAccessException.class);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(LegacyHoldMismatch.class)
    @DisplayName("hold 금융/만료 조건이 불완전하면 환불하고 Listing과 Inventory를 canonical available 상태로 복구한다")
    void recoversInvalidLegacyReservation(LegacyHoldMismatch mismatch) {
        jdbc.update("""
                UPDATE inventory_entries
                SET availability = 'RESERVED_FOR_TRADE'
                WHERE id = ?
                """, entryId);
        applyMismatch(mismatch);

        flyway(MigrationVersion.fromVersion("27")).migrate();

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM listing_reservations
                WHERE listing_id = ? AND state = 'ACTIVE'
                """, Integer.class, listingId)).isZero();
        assertThat(jdbc.queryForMap("""
                SELECT sale_quantity, status, reserved_by,
                       reservation_token, reservation_expires_at,
                       reserved_hold_id
                FROM listings
                WHERE id = ?
                """, listingId))
                .containsEntry("sale_quantity", null)
                .containsEntry("status", "OPEN")
                .containsEntry("reserved_by", null)
                .containsEntry("reservation_token", null)
                .containsEntry("reservation_expires_at", null)
                .containsEntry("reserved_hold_id", null);
        assertThat(jdbc.queryForObject("""
                SELECT status
                FROM wallet_holds
                WHERE hold_id = 'legacy-hold-296'
                """, String.class)).isEqualTo("CANCELED");
        assertThat(walletBalance(mismatch.refundCurrency()))
                .isEqualTo(mismatch.expectedBalance());
        assertThat(jdbc.queryForObject("""
                SELECT availability
                FROM inventory_entries
                WHERE id = ?
                """, String.class, entryId)).isEqualTo("LISTED");
    }

    private Flyway flyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(false)
                .target(target)
                .load();
    }

    private Long insertItem() {
        jdbc.update("""
                INSERT INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    'IT_296_MIGRATION', 'Issue 296 migration item',
                    'QUEST', 'ETC', 'COMMON', JSON_OBJECT(),
                    TRUE, 10, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        return jdbc.queryForObject(
                "SELECT id FROM items WHERE code = 'IT_296_MIGRATION'",
                Long.class
        );
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
                    FALSE, NULL, 8, 0, CURRENT_TIMESTAMP(6), ?, ?,
                    CURRENT_TIMESTAMP(6), JSON_OBJECT(), 'COMMON', 'LISTED'
                )
                """, itemId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM inventory_entries WHERE player_id = ?",
                Long.class,
                SELLER_ID
        );
    }

    private Long insertLegacyReservation(Long itemId, Long inventoryEntryId) {
        jdbc.update("""
                INSERT INTO listings (
                    active_flag, created_at, item_id, item_inst_id, price,
                    reservation_expires_at, reserved_by, seller_player_id,
                    updated_at, version, reservation_token, reserved_hold_id,
                    currency, status
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6), ?, ?, 80,
                    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR), 296102, ?,
                    CURRENT_TIMESTAMP(6), 0, 'legacy-token-296',
                    'legacy-hold-296', 'GOLD', 'RESERVED'
                )
                """, itemId, inventoryEntryId, SELLER_ID);
        return jdbc.queryForObject(
                "SELECT id FROM listings WHERE item_inst_id = ?",
                Long.class,
                inventoryEntryId
        );
    }

    private void insertBuyerWalletAndHold() {
        jdbc.update("""
                INSERT INTO wallets (created_at, owner_id, updated_at, version)
                VALUES (CURRENT_TIMESTAMP(6), 296102, CURRENT_TIMESTAMP(6), 0)
                """);
        Long walletId = jdbc.queryForObject(
                "SELECT id FROM wallets WHERE owner_id = 296102",
                Long.class
        );
        jdbc.update("""
                INSERT INTO wallet_balances (
                    amount, created_at, updated_at, wallet_id, currency
                ) VALUES (20, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?, 'GOLD')
                """, walletId);
        jdbc.update("""
                INSERT INTO wallet_holds (
                    amount, created_at, expires_at, updated_at, wallet_id,
                    hold_id, reason, currency, status
                ) VALUES (
                    80, CURRENT_TIMESTAMP(6),
                    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 2 HOUR),
                    CURRENT_TIMESTAMP(6), ?, 'legacy-hold-296',
                    'legacy listing reservation', 'GOLD', 'OPEN'
                )
                """, walletId);
    }

    private void applyMismatch(LegacyHoldMismatch mismatch) {
        switch (mismatch) {
            case AMOUNT -> jdbc.update("""
                    UPDATE wallet_holds
                    SET amount = 70
                    WHERE hold_id = 'legacy-hold-296'
                    """);
            case CURRENCY -> {
                Long walletId = jdbc.queryForObject(
                        "SELECT id FROM wallets WHERE owner_id = 296102",
                        Long.class
                );
                jdbc.update("""
                        INSERT INTO wallet_balances (
                            amount, created_at, updated_at,
                            wallet_id, currency
                        ) VALUES (
                            20, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
                            ?, 'GEM'
                        )
                        """, walletId);
                jdbc.update("""
                        UPDATE wallet_holds
                        SET currency = 'GEM'
                        WHERE hold_id = 'legacy-hold-296'
                        """);
            }
            case EXPIRY -> jdbc.update("""
                    UPDATE wallet_holds hold
                    JOIN listings listing
                      ON listing.reserved_hold_id = hold.hold_id
                    SET hold.expires_at = DATE_SUB(
                        listing.reservation_expires_at,
                        INTERVAL 1 SECOND
                    )
                    WHERE hold.hold_id = 'legacy-hold-296'
                    """);
        }
    }

    private long walletBalance(String currency) {
        return jdbc.queryForObject("""
                SELECT balance.amount
                FROM wallet_balances balance
                JOIN wallets wallet ON wallet.id = balance.wallet_id
                WHERE wallet.owner_id = 296102
                  AND balance.currency = ?
                """, Long.class, currency);
    }

    private void insertActiveReservation(String token) {
        jdbc.update("""
                INSERT INTO listing_reservations (
                    active_flag, created_at, expires_at, buyer_player_id,
                    listing_id, updated_at, version, reservation_token,
                    wallet_hold_id, state
                ) VALUES (
                    1, CURRENT_TIMESTAMP(6), DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 1 HOUR),
                    296103, ?, CURRENT_TIMESTAMP(6), 0, ?, 'second-hold-296', 'ACTIVE'
                )
                """, listingId, token);
    }

    private enum LegacyHoldMismatch {
        AMOUNT("GOLD", 90L),
        CURRENCY("GEM", 100L),
        EXPIRY("GOLD", 100L);

        private final String refundCurrency;
        private final long expectedBalance;

        LegacyHoldMismatch(String refundCurrency, long expectedBalance) {
            this.refundCurrency = refundCurrency;
            this.expectedBalance = expectedBalance;
        }

        String refundCurrency() {
            return refundCurrency;
        }

        long expectedBalance() {
            return expectedBalance;
        }
    }
}
