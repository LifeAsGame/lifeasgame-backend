package online.lifeasgame.inventory.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryEquipmentReadApi;
import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.InstanceAttrs;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCarryPolicy;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemName;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.PlayerInventory;
import online.lifeasgame.inventory.domain.Rarity;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties =
        "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
@DisplayName("Inventory equipment read provider")
class InventoryEquipmentReadIntegrationTest {

    private static final Long PLAYER_ID = 262L;
    private static final Long OTHER_PLAYER_ID = 263L;

    @Autowired
    private InventoryEquipmentReadApi inventoryEquipmentReadApi;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Nested
    @DisplayName("Player가 item instance를 소유하면")
    class OwnedItem {

        @Test
        @DisplayName("한 번의 direct query로 exact category와 type을 반환한다")
        void returnsCompactOwnedProjectionWithoutNPlusOne() {
            Long itemInstanceId = entry(
                    PLAYER_ID,
                    "소유한 투구",
                    ItemCategory.ARMOR,
                    ItemType.HELMET
            );
            flushAndClear();
            Statistics statistics = statistics();
            statistics.clear();

            InventoryEquipmentReadApi.OwnedEquipmentItem result =
                    inventoryEquipmentReadApi.getOwnedItem(
                            PLAYER_ID,
                            itemInstanceId
                    );

            assertThat(result.itemInstanceId()).isEqualTo(itemInstanceId);
            assertThat(result.itemId()).isPositive();
            assertThat(result.category()).isEqualTo("ARMOR");
            assertThat(result.type()).isEqualTo("HELMET");
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("item instance를 소유하지 않으면")
    class UnavailableItem {

        @Test
        @DisplayName("foreign과 nonexistent를 같은 ownership-safe not-found로 반환한다")
        void hidesForeignOwnershipAndMissingIdentity() {
            Long foreignItemInstanceId = entry(
                    OTHER_PLAYER_ID,
                    "다른 Player 검",
                    ItemCategory.WEAPON,
                    ItemType.SWORD
            );
            flushAndClear();

            assertNotFound(foreignItemInstanceId);
            assertNotFound(foreignItemInstanceId + 100_000);
        }
    }

    private Long entry(
            Long playerId,
            String name,
            ItemCategory category,
            ItemType type
    ) {
        Item item = Item.create(
                ItemName.of(name),
                category,
                type,
                Rarity.COMMON,
                BaseAttrs.empty(),
                false,
                null,
                null
        );
        entityManager.persist(item);
        entityManager.flush();
        PlayerInventory inventory = PlayerInventory.of(playerId, 10);
        inventory.add(
                ItemCarryPolicy.from(item),
                1,
                InstanceAttrs.empty(),
                false
        );
        entityManager.persist(inventory);
        entityManager.flush();
        return inventory.getEntries().getFirst().getId();
    }

    private void assertNotFound(Long itemInstanceId) {
        assertThatThrownBy(() -> inventoryEquipmentReadApi.getOwnedItem(
                PLAYER_ID,
                itemInstanceId
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(
                        InventoryError.INVENTORY_ENTRY_NOT_FOUND
                )
        );
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class)
                .getStatistics();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
