package online.lifeasgame.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DisplayName("V26 Inventory entry availability migration")
class InventoryAvailabilityFlywayTest {

    private static final Long PLAYER_ID = 294101L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.39")
                    .withDatabaseName("lifeasgame_inventory_availability")
                    .withUsername("lifeasgame")
                    .withPassword("lifeasgame");

    private JdbcTemplate jdbc;
    private Long itemId;
    private Long equippedEntryId;
    private Long freeEntryId;

    @BeforeEach
    void migrateThroughV25() {
        Flyway throughV25 = flyway(MigrationVersion.fromVersion("25"));
        throughV25.clean();
        throughV25.migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(),
                MYSQL.getUsername(),
                MYSQL.getPassword()
        ));
        itemId = insertItem();
        insertInventory();
        equippedEntryId = insertEntry(0, 1);
        freeEntryId = insertEntry(1, 4);
        insertEquipment(equippedEntryId);
    }

    @Test
    @DisplayName("기존 equipped row를 EQUIPPED로, 나머지를 FREE로 backfill하고 canonical constraint를 적용한다")
    void backfillsExistingRowsAndAddsConstraint() {
        var result = flyway(MigrationVersion.fromVersion("26")).migrate();

        assertThat(result.migrationsExecuted).isEqualTo(1);
        assertThat(availability(equippedEntryId)).isEqualTo("EQUIPPED");
        assertThat(availability(freeEntryId)).isEqualTo("FREE");
        assertThat(jdbc.queryForMap("""
                SELECT is_nullable, column_default, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_entries'
                  AND column_name = 'availability'
                """)).containsEntry("is_nullable", "NO")
                .containsEntry("column_default", "FREE")
                .containsEntry("character_maximum_length", 32L);

        Long defaultedEntryId = insertEntry(2, 2);
        assertThat(availability(defaultedEntryId)).isEqualTo("FREE");
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE inventory_entries
                SET availability = 'UNKNOWN'
                WHERE id = ?
                """, defaultedEntryId))
                .isInstanceOf(DataAccessException.class);
    }

    private Flyway flyway(MigrationVersion target) {
        return Flyway.configure()
                .dataSource(
                        MYSQL.getJdbcUrl(),
                        MYSQL.getUsername(),
                        MYSQL.getPassword()
                )
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
                    'IT_294_MIGRATION', 'Issue 294 migration item',
                    'QUEST', 'ETC', 'COMMON', JSON_OBJECT(),
                    TRUE, 10, NULL,
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """);
        return jdbc.queryForObject(
                "SELECT id FROM items WHERE code = 'IT_294_MIGRATION'",
                Long.class
        );
    }

    private void insertInventory() {
        jdbc.update("""
                INSERT INTO player_inventory (
                    player_id, capacity_slots, version,
                    created_at, updated_at
                ) VALUES (
                    ?, 10, 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
                )
                """, PLAYER_ID);
    }

    private Long insertEntry(int slot, int quantity) {
        jdbc.update("""
                INSERT INTO inventory_entries (
                    bound, durability, quantity, slot_index,
                    created_at, item_id, player_id, updated_at,
                    inst_attrs, rarity
                ) VALUES (
                    FALSE, NULL, ?, ?, CURRENT_TIMESTAMP(6),
                    ?, ?, CURRENT_TIMESTAMP(6), JSON_OBJECT(), 'COMMON'
                )
                """, quantity, slot, itemId, PLAYER_ID);
        return jdbc.queryForObject("""
                SELECT id
                FROM inventory_entries
                WHERE player_id = ? AND slot_index = ?
                """, Long.class, PLAYER_ID, slot);
    }

    private void insertEquipment(Long inventoryEntryId) {
        jdbc.update("""
                INSERT INTO player_equipment (
                    created_at, equipped_at, item_inst_id,
                    player_id, slot_id, updated_at
                ) VALUES (
                    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), ?,
                    ?, 1, CURRENT_TIMESTAMP(6)
                )
                """, inventoryEntryId, PLAYER_ID);
    }

    private String availability(Long inventoryEntryId) {
        return jdbc.queryForObject("""
                SELECT availability
                FROM inventory_entries
                WHERE id = ?
                """, String.class, inventoryEntryId);
    }
}
