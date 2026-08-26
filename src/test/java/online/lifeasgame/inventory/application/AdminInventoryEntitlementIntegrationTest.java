package online.lifeasgame.inventory.application;

import online.lifeasgame.adminaudit.domain.error.AdminAuditError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentUserAccessor;
import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemName;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Admin Inventory entitlement MySQL transaction contract")
class AdminInventoryEntitlementIntegrationTest {

    private static final long ADMIN_ID = 9310L;
    private static final long PLAYER_ID = 19310L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_admin_entitlement")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame")
                    .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName
        );
        registry.add("app.outbox.enabled", () -> false);
    }

    @Autowired
    private AdminInventoryEntitlementService entitlementService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentUserAccessor currentUserAccessor;

    private Long itemId;

    @BeforeEach
    void setUp() {
        jdbc.execute("DROP TRIGGER IF EXISTS force_admin_entitlement_audit_failure");
        jdbc.update("DELETE FROM admin_audit_events");
        jdbc.update("DELETE FROM outbox_events");
        jdbc.update("DELETE FROM inventory_entries WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM mailbox_entries WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM player_inventory WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM player_mailbox WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM items WHERE name = 'Admin entitlement test item'");
        jdbc.update("DELETE FROM users WHERE id = ?", ADMIN_ID);
        jdbc.update("""
                INSERT INTO users (
                    id, email, password_hash, nickname, status,
                    account_authority, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'ACTIVE', 'ADMIN', NOW(6), NOW(6))
                """, ADMIN_ID, "entitlement-admin@example.com", "hash", "entitlement-admin");
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 60, 0, NOW(6), NOW(6))
                """, PLAYER_ID);
        jdbc.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, 100, 0, NOW(6), NOW(6))
                """, PLAYER_ID);
        jdbc.execute("""
                CREATE TRIGGER force_admin_entitlement_audit_failure
                BEFORE INSERT ON admin_audit_events
                FOR EACH ROW
                SET NEW.actor_user_id = IF(
                    NEW.correlation_id = 'force-audit-failure',
                    999999999,
                    NEW.actor_user_id
                )
                """);
        given(currentUserAccessor.currentUserIdOrThrow()).willReturn(ADMIN_ID);

        Item item = itemRepository.save(Item.create(
                ItemName.of("Admin entitlement test item"),
                ItemCategory.MISC,
                ItemType.ETC,
                null,
                Rarity.COMMON,
                BaseAttrs.empty(),
                false,
                null,
                null
        ));
        itemId = item.getId();
    }

    @Test
    @DisplayName("두 action의 sequential duplicate는 각각 entitlement를 한 번만 commit한다")
    void rejectsSequentialDuplicates() {
        entitlementService.addToInventory(inventory(
                "shared-sequential-key",
                "request-inventory-1"
        ));
        entitlementService.deliverToMailbox(mailbox(
                "shared-sequential-key",
                "request-mailbox-1"
        ));

        assertDuplicate(() -> entitlementService.addToInventory(inventory(
                "shared-sequential-key",
                "request-inventory-2"
        )));
        assertDuplicate(() -> entitlementService.deliverToMailbox(mailbox(
                "shared-sequential-key",
                "request-mailbox-2"
        )));

        assertEntitlementState(1, 1);
        assertThat(successAuditCount("INVENTORY_ITEM_ADD")).isEqualTo(1);
        assertThat(successAuditCount("MAILBOX_ITEM_DELIVERY")).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
        assertThat(outboxTypes()).containsExactly("inventory.item-added.v1");
        assertThat(auditRow("INVENTORY_ITEM_ADD")).containsExactly(
                ADMIN_ID,
                "INVENTORY_ITEM_ADD",
                "PLAYER_INVENTORY",
                Long.toString(PLAYER_ID),
                "CASE-310",
                "SUCCESS",
                "request-inventory-1",
                "shared-sequential-key"
        );
        assertThat(auditRow("MAILBOX_ITEM_DELIVERY")).containsExactly(
                ADMIN_ID,
                "MAILBOX_ITEM_DELIVERY",
                "PLAYER_MAILBOX",
                Long.toString(PLAYER_ID),
                "CASE-310",
                "SUCCESS",
                "request-mailbox-1",
                "shared-sequential-key"
        );
    }

    @Test
    @DisplayName("두 action의 concurrent same key는 각각 entitlement를 한 번만 commit한다")
    void rejectsConcurrentDuplicates() throws Exception {
        List<Outcome> inventoryOutcomes = concurrently(
                () -> entitlementService.addToInventory(inventory(
                        "shared-concurrent-key",
                        "request-inventory-1"
                )),
                () -> entitlementService.addToInventory(inventory(
                        "shared-concurrent-key",
                        "request-inventory-2"
                ))
        );
        assertExactlyOneDuplicate(inventoryOutcomes);

        List<Outcome> mailboxOutcomes = concurrently(
                () -> entitlementService.deliverToMailbox(mailbox(
                        "shared-concurrent-key",
                        "request-mailbox-1"
                )),
                () -> entitlementService.deliverToMailbox(mailbox(
                        "shared-concurrent-key",
                        "request-mailbox-2"
                ))
        );
        assertExactlyOneDuplicate(mailboxOutcomes);

        assertEntitlementState(1, 1);
        assertThat(successAuditCount("INVENTORY_ITEM_ADD")).isEqualTo(1);
        assertThat(successAuditCount("MAILBOX_ITEM_DELIVERY")).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("aggregate row lock은 concurrent capacity boundary에서 한 command만 허용한다")
    void serializesCapacityBoundary() throws Exception {
        setCapacity("player_inventory", 1);
        setCapacity("player_mailbox", 1);

        List<Outcome> inventoryOutcomes = concurrently(
                () -> entitlementService.addToInventory(inventory(
                        "inventory-capacity-1",
                        "request-inventory-1"
                )),
                () -> entitlementService.addToInventory(inventory(
                        "inventory-capacity-2",
                        "request-inventory-2"
                ))
        );
        assertOneCapacityFailure(
                inventoryOutcomes,
                InventoryError.INVENTORY_FULL
        );

        List<Outcome> mailboxOutcomes = concurrently(
                () -> entitlementService.deliverToMailbox(mailbox(
                        "mailbox-capacity-1",
                        "request-mailbox-1"
                )),
                () -> entitlementService.deliverToMailbox(mailbox(
                        "mailbox-capacity-2",
                        "request-mailbox-2"
                ))
        );
        assertOneCapacityFailure(
                mailboxOutcomes,
                InventoryError.MAILBOX_FULL
        );

        assertEntitlementState(1, 1);
        assertThat(successAuditCount("INVENTORY_ITEM_ADD")).isEqualTo(1);
        assertThat(successAuditCount("MAILBOX_ITEM_DELIVERY")).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("capacity failure는 success key를 소비하지 않아 state 보정 후 retry할 수 있다")
    void retriesAfterCapacityFailure() {
        setCapacity("player_inventory", 1);
        setCapacity("player_mailbox", 1);
        insertInventoryEntry();
        insertMailboxEntry();

        assertBusinessFailure(
                () -> entitlementService.addToInventory(inventory(
                        "inventory-capacity-retry",
                        "request-inventory-failure"
                )),
                InventoryError.INVENTORY_FULL
        );
        assertBusinessFailure(
                () -> entitlementService.deliverToMailbox(mailbox(
                        "mailbox-capacity-retry",
                        "request-mailbox-failure"
                )),
                InventoryError.MAILBOX_FULL
        );
        assertThat(auditCount()).isZero();
        assertThat(outboxCount()).isZero();

        jdbc.update("DELETE FROM inventory_entries WHERE player_id = ?", PLAYER_ID);
        jdbc.update("DELETE FROM mailbox_entries WHERE player_id = ?", PLAYER_ID);
        entitlementService.addToInventory(inventory(
                "inventory-capacity-retry",
                "request-inventory-retry"
        ));
        entitlementService.deliverToMailbox(mailbox(
                "mailbox-capacity-retry",
                "request-mailbox-retry"
        ));

        assertEntitlementState(1, 1);
        assertThat(auditCount()).isEqualTo(2);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Audit failure는 Inventory/Mailbox entitlement와 outbox를 rollback한다")
    void rollsBackAuditFailure() {
        assertThatThrownBy(() -> entitlementService.addToInventory(inventory(
                "inventory-audit-failure",
                "force-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> entitlementService.deliverToMailbox(mailbox(
                "mailbox-audit-failure",
                "force-audit-failure"
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertEntitlementState(0, 0);
        assertThat(auditCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    private AdminInventoryEntitlementCommand.AddToInventory inventory(
            String key,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.AddToInventory(
                PLAYER_ID,
                itemId,
                1,
                true,
                "CASE-310",
                key,
                correlationId
        );
    }

    private AdminInventoryEntitlementCommand.DeliverToMailbox mailbox(
            String key,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.DeliverToMailbox(
                PLAYER_ID,
                itemId,
                1,
                true,
                "CASE-310",
                key,
                correlationId
        );
    }

    private List<Outcome> concurrently(Runnable first, Runnable second)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstResult = executor.submit(
                    () -> invoke(ready, start, first)
            );
            Future<Outcome> secondResult = executor.submit(
                    () -> invoke(ready, start, second)
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Outcome invoke(
            CountDownLatch ready,
            CountDownLatch start,
            Runnable command
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                return new Outcome(false, new IllegalStateException("start timeout"));
            }
            command.run();
            return new Outcome(true, null);
        } catch (Throwable exception) {
            return new Outcome(false, exception);
        }
    }

    private void assertExactlyOneDuplicate(List<Outcome> outcomes) {
        assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                .singleElement()
                .satisfies(outcome -> assertDuplicate(outcome.failure()));
    }

    private void assertOneCapacityFailure(
            List<Outcome> outcomes,
            InventoryError error
    ) {
        assertThat(outcomes).filteredOn(Outcome::success).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> !outcome.success())
                .singleElement()
                .satisfies(outcome -> assertBusinessFailure(
                        outcome.failure(),
                        error
                ));
    }

    private void assertDuplicate(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        this::assertDuplicate
                );
    }

    private void assertDuplicate(Throwable failure) {
        assertThat(failure).isInstanceOf(DomainException.class);
        assertThat(((DomainException) failure).getErrorCode()).isEqualTo(
                AdminAuditError.DUPLICATE_IDEMPOTENCY_KEY
        );
    }

    private void assertBusinessFailure(Runnable command, InventoryError error) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(error)
                );
    }

    private void assertBusinessFailure(Throwable failure, InventoryError error) {
        assertThat(failure).isInstanceOf(DomainException.class);
        assertThat(((DomainException) failure).getErrorCode()).isEqualTo(error);
    }

    private void assertEntitlementState(
            int expectedInventoryEntries,
            int expectedMailboxEntries
    ) {
        assertThat(entryCount("inventory_entries"))
                .isEqualTo(expectedInventoryEntries);
        assertThat(entryCount("mailbox_entries"))
                .isEqualTo(expectedMailboxEntries);
        if (expectedInventoryEntries > 0) {
            assertThat(canonicalEntryCount("inventory_entries"))
                    .isEqualTo(expectedInventoryEntries);
        }
        if (expectedMailboxEntries > 0) {
            assertThat(canonicalEntryCount("mailbox_entries"))
                    .isEqualTo(expectedMailboxEntries);
        }
    }

    private int entryCount(String table) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE player_id = ?",
                Integer.class,
                PLAYER_ID
        );
    }

    private int canonicalEntryCount(String table) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table
                        + " WHERE player_id = ? AND quantity = 1"
                        + " AND bound = TRUE AND JSON_LENGTH(inst_attrs) = 0",
                Integer.class,
                PLAYER_ID
        );
    }

    private int auditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events",
                Integer.class
        );
    }

    private int successAuditCount(String action) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM admin_audit_events
                WHERE action = ? AND result = 'SUCCESS'
                """, Integer.class, action);
    }

    private int outboxCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Integer.class
        );
    }

    private List<String> outboxTypes() {
        return jdbc.queryForList(
                "SELECT event_type FROM outbox_events ORDER BY id",
                String.class
        );
    }

    private List<Object> auditRow(String action) {
        return jdbc.queryForObject("""
                SELECT actor_user_id, action, target_type, target_id,
                       reason, result, correlation_id, idempotency_key
                FROM admin_audit_events
                WHERE action = ?
                """, (result, row) -> List.of(
                result.getLong("actor_user_id"),
                result.getString("action"),
                result.getString("target_type"),
                result.getString("target_id"),
                result.getString("reason"),
                result.getString("result"),
                result.getString("correlation_id"),
                result.getString("idempotency_key")
        ), action);
    }

    private void setCapacity(String table, int capacity) {
        jdbc.update(
                "UPDATE " + table + " SET capacity_slots = ? WHERE player_id = ?",
                capacity,
                PLAYER_ID
        );
    }

    private void insertInventoryEntry() {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index, created_at,
                    item_id, player_id, updated_at, inst_attrs, rarity
                ) VALUES (TRUE, NULL, 1, 0, NOW(6), ?, ?, NOW(6), JSON_OBJECT(), 'COMMON')
                """, itemId, PLAYER_ID);
    }

    private void insertMailboxEntry() {
        jdbc.update("""
                INSERT INTO mailbox_entries (
                    bound, durability, quantity, slot_index, created_at,
                    item_id, player_id, updated_at, inst_attrs, rarity
                ) VALUES (TRUE, NULL, 1, 0, NOW(6), ?, ?, NOW(6), JSON_OBJECT(), 'COMMON')
                """, itemId, PLAYER_ID);
    }

    private record Outcome(boolean success, Throwable failure) {
    }
}
