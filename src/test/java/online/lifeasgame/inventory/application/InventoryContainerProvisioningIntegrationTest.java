package online.lifeasgame.inventory.application;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.PlayerMailbox;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Inventory container provisioning integration")
class InventoryContainerProvisioningIntegrationTest {

    private static final Long PLAYER_ID = 224001L;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.39")
            .withDatabaseName("lifeasgame_inventory_provisioning")
            .withUsername("lifeasgame")
            .withPassword("lifeasgame");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private InventoryContainerProvisioningService provisioningService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanContainers() {
        jdbcTemplate.update("DELETE FROM inventory_entries");
        jdbcTemplate.update("DELETE FROM mailbox_entries");
        jdbcTemplate.update("DELETE FROM player_inventory");
        jdbcTemplate.update("DELETE FROM player_mailbox");
    }

    @Test
    @DisplayName("둘 다 없으면 기본 capacity로 만들고 순차 replay는 no-op이다")
    void ensuresMissingContainersIdempotently() {
        provisioningService.ensureContainers(PLAYER_ID);
        provisioningService.ensureContainers(PLAYER_ID);

        assertContainerCounts(PLAYER_ID, 1, 1);
        assertThat(inventoryCapacity(PLAYER_ID))
                .isEqualTo(PlayerInventory.DEFAULT_CAPACITY);
        assertThat(mailboxCapacity(PLAYER_ID))
                .isEqualTo(PlayerMailbox.DEFAULT_CAPACITY);
    }

    @Test
    @DisplayName("Inventory만 있으면 기존 capacity와 version을 보존한다")
    void preservesExistingInventory() {
        insertInventory(PLAYER_ID, 77, 8L);

        provisioningService.ensureContainers(PLAYER_ID);

        assertContainerCounts(PLAYER_ID, 1, 1);
        assertThat(inventoryCapacity(PLAYER_ID)).isEqualTo(77);
        assertThat(inventoryVersion(PLAYER_ID)).isEqualTo(8L);
        assertThat(mailboxCapacity(PLAYER_ID))
                .isEqualTo(PlayerMailbox.DEFAULT_CAPACITY);
    }

    @Test
    @DisplayName("Mailbox만 있으면 기존 capacity와 version을 보존한다")
    void preservesExistingMailbox() {
        insertMailbox(PLAYER_ID, 133, 9L);

        provisioningService.ensureContainers(PLAYER_ID);

        assertContainerCounts(PLAYER_ID, 1, 1);
        assertThat(mailboxCapacity(PLAYER_ID)).isEqualTo(133);
        assertThat(mailboxVersion(PLAYER_ID)).isEqualTo(9L);
        assertThat(inventoryCapacity(PLAYER_ID))
                .isEqualTo(PlayerInventory.DEFAULT_CAPACITY);
    }

    @Test
    @DisplayName("둘 다 있으면 custom capacity와 version을 모두 보존한다")
    void preservesBothExistingContainers() {
        insertInventory(PLAYER_ID, 71, 4L);
        insertMailbox(PLAYER_ID, 121, 5L);

        provisioningService.ensureContainers(PLAYER_ID);

        assertContainerCounts(PLAYER_ID, 1, 1);
        assertThat(inventoryCapacity(PLAYER_ID)).isEqualTo(71);
        assertThat(inventoryVersion(PLAYER_ID)).isEqualTo(4L);
        assertThat(mailboxCapacity(PLAYER_ID)).isEqualTo(121);
        assertThat(mailboxVersion(PLAYER_ID)).isEqualTo(5L);
    }

    @Test
    @DisplayName("동시 ensure에도 각 container는 한 건이다")
    void ensuresConcurrently() throws Exception {
        int workers = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    provisioningService.ensureContainers(PLAYER_ID);
                    return null;
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertContainerCounts(PLAYER_ID, 1, 1);
        assertThat(inventoryCapacity(PLAYER_ID))
                .isEqualTo(PlayerInventory.DEFAULT_CAPACITY);
        assertThat(mailboxCapacity(PLAYER_ID))
                .isEqualTo(PlayerMailbox.DEFAULT_CAPACITY);
    }

    @Test
    @DisplayName("playerId는 양수여야 한다")
    void rejectsInvalidPlayerId() {
        assertThatThrownBy(() -> provisioningService.ensureContainers(0L))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InventoryError.PLAYER_ID_INVALID)
                );

        assertContainerCounts(0L, 0, 0);
    }

    private void insertInventory(Long playerId, int capacity, Long version) {
        jdbcTemplate.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, capacity, version);
    }

    private void insertMailbox(Long playerId, int capacity, Long version) {
        jdbcTemplate.update("""
                INSERT INTO player_mailbox (
                    player_id, capacity_slots, version, created_at, updated_at
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, playerId, capacity, version);
    }

    private void assertContainerCounts(
            Long playerId,
            int inventoryCount,
            int mailboxCount
    ) {
        assertThat(count("player_inventory", playerId)).isEqualTo(inventoryCount);
        assertThat(count("player_mailbox", playerId)).isEqualTo(mailboxCount);
    }

    private int count(String table, Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }

    private int inventoryCapacity(Long playerId) {
        return capacity("player_inventory", playerId);
    }

    private int mailboxCapacity(Long playerId) {
        return capacity("player_mailbox", playerId);
    }

    private int capacity(String table, Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT capacity_slots FROM " + table + " WHERE player_id = ?",
                Integer.class,
                playerId
        );
    }

    private Long inventoryVersion(Long playerId) {
        return version("player_inventory", playerId);
    }

    private Long mailboxVersion(Long playerId) {
        return version("player_mailbox", playerId);
    }

    private Long version(String table, Long playerId) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM " + table + " WHERE player_id = ?",
                Long.class,
                playerId
        );
    }
}
