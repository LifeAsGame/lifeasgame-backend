package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Inventory entry availability")
class InventoryAvailabilityTest {

    private static final ItemCarryPolicy STACKABLE = new ItemCarryPolicy(
            294L,
            Rarity.COMMON,
            true,
            10,
            null
    );

    @Nested
    @DisplayName("market 상태를 전이할 때")
    class MarketTransitions {

        @Test
        @DisplayName("확정된 whole-entry 전이만 허용한다")
        void allowsOnlyRequiredTransitions() {
            InventoryEntry entry = entry(5);

            entry.listForMarket();
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.LISTED);
            entry.reserveForTrade();
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.RESERVED_FOR_TRADE);
            entry.releaseTradeReservation();
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.LISTED);
            entry.releaseListing();
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.FREE);

            entry.listForMarket();
            entry.reserveForTrade();
            entry.beginTransfer();
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.TRANSFER_PROCESSING);
        }

        @Test
        @DisplayName("허용되지 않은 전이는 stable Inventory error로 거절한다")
        void rejectsInvalidTransition() {
            InventoryEntry entry = entry(5);
            entry.listForMarket();

            assertThatThrownBy(entry::listForMarket)
                    .isInstanceOfSatisfying(
                            DomainException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(InventoryError.INVALID_AVAILABILITY_TRANSITION)
                    );
            assertThat(entry.getAvailability())
                    .isEqualTo(InventoryAvailability.LISTED);
        }
    }

    @Nested
    @DisplayName("ordinary mutation을 시도할 때")
    class OrdinaryMutationGuard {

        @Test
        @DisplayName("LISTED와 RESERVED_FOR_TRADE entry의 quantity 변경을 거절한다")
        void rejectsOccupiedQuantityMutation() {
            InventoryEntry listed = entry(5);
            listed.listForMarket();
            assertUnavailable(() -> listed.decreaseQuantity(1));

            InventoryEntry reserved = entry(5);
            reserved.listForMarket();
            reserved.reserveForTrade();
            assertUnavailable(() -> reserved.increaseQuantity(1, STACKABLE));
        }

        @Test
        @DisplayName("EQUIPPED는 listing할 수 없고 LISTED는 equip할 수 없다")
        void keepsEquipmentAndMarketMutuallyExclusive() {
            InventoryEntry equipped = entry(1);
            equipped.markEquipped();
            assertInvalidTransition(equipped::listForMarket);

            InventoryEntry listed = entry(1);
            listed.listForMarket();
            assertInvalidTransition(listed::markEquipped);
        }
    }

    @Nested
    @DisplayName("normal add와 capacity preflight를 수행할 때")
    class FreeOnlyMergeEligibility {

        @Test
        @DisplayName("LISTED stack을 merge 대상으로 쓰지 않고 새 FREE stack을 만든다")
        void skipsListedStackInPreflightAndActualAdd() {
            PlayerInventory inventory = PlayerInventory.of(294L, 2);
            inventory.add(STACKABLE, 9, InstanceAttrs.empty(), true);
            InventoryEntry listed = inventory.getEntries().getFirst();
            listed.listForMarket();

            inventory.assertCanAdd(
                    STACKABLE,
                    1,
                    InstanceAttrs.empty(),
                    true
            );
            inventory.add(STACKABLE, 1, InstanceAttrs.empty(), true);

            assertThat(inventory.getEntries()).hasSize(2);
            assertThat(listed.getQuantity().value()).isEqualTo(9);
            assertThat(inventory.getEntries().get(1).getQuantity().value())
                    .isEqualTo(1);
            assertThat(inventory.getEntries().get(1).getAvailability())
                    .isEqualTo(InventoryAvailability.FREE);
        }

        @Test
        @DisplayName("LISTED stack만 남은 full inventory는 preflight에서 거절한다")
        void rejectsCapacityThatOnlyListedStackCouldAbsorb() {
            PlayerInventory inventory = PlayerInventory.of(294L, 1);
            inventory.add(STACKABLE, 9, InstanceAttrs.empty(), true);
            inventory.getEntries().getFirst().listForMarket();

            assertThatThrownBy(() -> inventory.assertCanAdd(
                    STACKABLE,
                    1,
                    InstanceAttrs.empty(),
                    true
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(InventoryError.INVENTORY_FULL)
            );
        }
    }

    private InventoryEntry entry(int quantity) {
        PlayerInventory inventory = PlayerInventory.of(294L, 10);
        inventory.add(
                STACKABLE,
                quantity,
                InstanceAttrs.empty(),
                true
        );
        return inventory.getEntries().getFirst();
    }

    private void assertUnavailable(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(InventoryError.INVENTORY_ENTRY_UNAVAILABLE)
                );
    }

    private void assertInvalidTransition(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(InventoryError.INVALID_AVAILABILITY_TRANSITION)
                );
    }
}
