package online.lifeasgame.character.application;

import online.lifeasgame.character.application.command.PlayerEquipmentCommand;
import online.lifeasgame.character.domain.error.PlayerEquipmentError;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest
@ActiveProfiles({"test", "migration-test"})
@DisplayName("Player equipment concurrency MySQL integration")
class PlayerEquipmentConcurrencyIntegrationTest {

    private static final Long PLAYER_ID = 262001L;
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_equipment_concurrency")
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
    private PlayerEquipmentService playerEquipmentService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    private Long firstSlotId;
    private Long secondSlotId;
    private Long itemInstanceId;

    @BeforeEach
    void setUp() {
        given(currentPlayerAccessor.currentPlayerIdOrThrow())
                .willReturn(PLAYER_ID);
        jdbc.update("DELETE FROM player_equipment");
        jdbc.update("DELETE FROM inventory_entries");
        jdbc.update("DELETE FROM player_inventory");
        jdbc.update("DELETE FROM equipment_slots");
        jdbc.update("DELETE FROM items");
        firstSlotId = insertSlot("WEAPON_MAIN", "주무기", "MAIN");
        secondSlotId = insertSlot("WEAPON_OFF", "보조무기", "OFFHAND");
        itemInstanceId = insertOwnedWeapon();
        insertEmptyEquipment(firstSlotId);
        insertEmptyEquipment(secondSlotId);
    }

    @Nested
    @DisplayName("같은 item을 서로 다른 슬롯에 동시에 장착하면")
    class EquipSameItemAcrossSlots {

        @Test
        @DisplayName("정확히 하나만 성공하고 하나는 stable semantic conflict다")
        void allowsOneWinnerAndMapsOneConflict() throws Exception {
            List<Throwable> outcomes = race(
                    () -> equip(firstSlotId),
                    () -> equip(secondSlotId)
            );

            assertThat(outcomes.stream().filter(Objects::isNull).count())
                    .isEqualTo(1);
            Throwable failure = outcomes.stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
            assertThat(failure).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(
                                    PlayerEquipmentError.ALREADY_EQUIPPED_ITEM
                            )
            );
            assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM player_equipment
                    WHERE player_id = ?
                      AND item_inst_id = ?
                    """, Integer.class, PLAYER_ID, itemInstanceId))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("""
                    SELECT availability
                    FROM inventory_entries
                    WHERE id = ?
                    """, String.class, itemInstanceId))
                    .isEqualTo("EQUIPPED");
        }
    }

    private void equip(Long slotId) {
        playerEquipmentService.equip(new PlayerEquipmentCommand.Equip(
                slotId,
                itemInstanceId
        ));
    }

    private List<Throwable> race(
            Runnable first,
            Runnable second
    ) throws Exception {
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

    private Long insertSlot(String code, String name, String role) {
        jdbc.update("""
                INSERT INTO equipment_slots (
                    created_at, updated_at, code, name, category, role,
                    definition_version, enabled, lifecycle_status
                ) VALUES (
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6),
                    ?, ?, 'WEAPON', ?, 'LEGACY', b'1', 'ACTIVE'
                )
                """, code, name, role);
        return jdbc.queryForObject(
                "SELECT id FROM equipment_slots WHERE code = ?",
                Long.class,
                code
        );
    }

    private Long insertOwnedWeapon() {
        jdbc.update("""
                INSERT INTO items (
                    code, name, category, type, rarity, base_attrs,
                    stackable, max_stack, max_durability,
                    created_at, updated_at
                ) VALUES (
                    'IT_262_WEAPON', 'Issue 262 weapon',
                    'WEAPON', 'SWORD', 'COMMON', JSON_OBJECT(),
                    FALSE, 1, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        Long itemId = jdbc.queryForObject(
                "SELECT id FROM items WHERE code = 'IT_262_WEAPON'",
                Long.class
        );
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version,
                    created_at, updated_at
                ) VALUES (
                    ?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID);
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity
                ) VALUES (
                    FALSE, NULL, 1, 0,
                    CURRENT_TIMESTAMP(6), ?, ?, CURRENT_TIMESTAMP(6),
                    JSON_OBJECT(), 'COMMON'
                )
                """, itemId, PLAYER_ID);
        return jdbc.queryForObject("""
                SELECT id
                FROM inventory_entries
                WHERE player_id = ? AND item_id = ?
                """, Long.class, PLAYER_ID, itemId);
    }

    private void insertEmptyEquipment(Long slotId) {
        jdbc.update("""
                INSERT INTO player_equipment (
                    created_at, equipped_at, item_inst_id,
                    player_id, slot_id, updated_at
                ) VALUES (
                    CURRENT_TIMESTAMP(6), NULL, NULL,
                    ?, ?, CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID, slotId);
    }
}
