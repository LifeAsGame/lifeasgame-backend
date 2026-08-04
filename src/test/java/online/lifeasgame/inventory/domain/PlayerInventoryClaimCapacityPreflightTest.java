package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerInventory claim capacity preflight")
class PlayerInventoryClaimCapacityPreflightTest {

    private static final ItemCarryPolicy STACKABLE = new ItemCarryPolicy(
            10L,
            Rarity.COMMON,
            true,
            10,
            null
    );
    private static final ItemCarryPolicy OTHER = new ItemCarryPolicy(
            11L,
            Rarity.COMMON,
            true,
            10,
            null
    );

    @Test
    @DisplayName("앞선 claim의 stack과 slot 사용을 누적해 batch 전체를 거부한다")
    void rejectsAccumulatedCapacityWithoutMutation() {
        PlayerInventory inventory = PlayerInventory.of(1L, 2);
        inventory.add(STACKABLE, 9, InstanceAttrs.empty(), true);
        InventoryEntry existing = inventory.getEntries().getFirst();

        List<PlayerInventory.Addition> additions = List.of(
                addition(STACKABLE, 11, InstanceAttrs.empty(), true),
                addition(OTHER, 1, InstanceAttrs.empty(), true)
        );

        assertThatThrownBy(() -> inventory.assertCanAddAll(additions))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(InventoryError.INVENTORY_FULL)
                );

        assertThat(inventory.getEntries()).containsExactly(existing);
        assertThat(existing.getQuantity().value()).isEqualTo(9);
    }

    @Test
    @DisplayName("성공 simulation도 실제 stack과 slot을 변경하지 않는다")
    void simulatesStackAndNewSlotWithoutMutation() {
        PlayerInventory inventory = PlayerInventory.of(1L, 2);
        inventory.add(STACKABLE, 9, InstanceAttrs.empty(), true);

        inventory.assertCanAddAll(List.of(
                addition(STACKABLE, 1, InstanceAttrs.empty(), true),
                addition(STACKABLE, 10, InstanceAttrs.empty(), true)
        ));

        assertThat(inventory.getEntries()).hasSize(1);
        assertThat(inventory.getEntries().getFirst().getQuantity().value())
                .isEqualTo(9);
    }

    @Test
    @DisplayName("bound와 attrs가 다른 동일 Item은 서로 다른 slot으로 계산한다")
    void distinguishesStackKeys() {
        PlayerInventory inventory = PlayerInventory.of(1L, 2);
        InstanceAttrs first = InstanceAttrs.of(Map.of("grade", "A"));
        InstanceAttrs second = InstanceAttrs.of(Map.of("grade", "B"));

        inventory.assertCanAddAll(List.of(
                addition(STACKABLE, 1, first, true),
                addition(STACKABLE, 1, second, false)
        ));

        assertThatThrownBy(() -> inventory.assertCanAddAll(List.of(
                addition(STACKABLE, 1, first, true),
                addition(STACKABLE, 1, second, false),
                addition(OTHER, 1, InstanceAttrs.empty(), true)
        ))).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(InventoryError.INVENTORY_FULL)
        );
        assertThat(inventory.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("non-stackable Item은 quantity만큼 free slot을 사용한다")
    void simulatesNonStackableItems() {
        PlayerInventory inventory = PlayerInventory.of(1L, 2);
        ItemCarryPolicy nonStackable = new ItemCarryPolicy(
                12L,
                Rarity.COMMON,
                false,
                1,
                null
        );

        inventory.assertCanAdd(
                nonStackable,
                2,
                InstanceAttrs.empty(),
                false
        );
        assertThatThrownBy(() -> inventory.assertCanAdd(
                nonStackable,
                3,
                InstanceAttrs.empty(),
                false
        )).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(InventoryError.INVENTORY_FULL)
        );
        assertThat(inventory.getEntries()).isEmpty();
    }

    private PlayerInventory.Addition addition(
            ItemCarryPolicy policy,
            int quantity,
            InstanceAttrs attrs,
            boolean bound
    ) {
        return new PlayerInventory.Addition(policy, quantity, attrs, bound);
    }
}
