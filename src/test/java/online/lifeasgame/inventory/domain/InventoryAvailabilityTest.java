package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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

        @Test
        @DisplayName("bound entry는 market 전진을 거절하고 historical 상태 해제는 허용한다")
        void rejectsBoundForwardTransitionsAndAllowsRecovery() {
            InventoryEntry free = entry(5, true);
            assertBoundMarketRestricted(free::listForMarket);
            assertThat(free.getAvailability()).isEqualTo(InventoryAvailability.FREE);

            InventoryEntry listed = entry(5, true);
            ReflectionTestUtils.setField(listed, "availability", InventoryAvailability.LISTED);
            assertBoundMarketRestricted(listed::reserveForTrade);
            assertThat(listed.getAvailability()).isEqualTo(InventoryAvailability.LISTED);
            listed.releaseListing();
            assertThat(listed.getAvailability()).isEqualTo(InventoryAvailability.FREE);

            InventoryEntry reserved = entry(5, true);
            ReflectionTestUtils.setField(
                    reserved,
                    "availability",
                    InventoryAvailability.RESERVED_FOR_TRADE
            );
            assertBoundMarketRestricted(reserved::beginTransfer);
            assertThat(reserved.getAvailability())
                    .isEqualTo(InventoryAvailability.RESERVED_FOR_TRADE);
            reserved.releaseTradeReservation();
            reserved.releaseListing();
            assertThat(reserved.getAvailability()).isEqualTo(InventoryAvailability.FREE);
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
            inventory.add(STACKABLE, 9, InstanceAttrs.empty(), false);
            InventoryEntry listed = inventory.getEntries().getFirst();
            listed.listForMarket();

            inventory.assertCanAdd(
                    STACKABLE,
                    1,
                    InstanceAttrs.empty(),
                    false
            );
            inventory.add(STACKABLE, 1, InstanceAttrs.empty(), false);

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
            inventory.add(STACKABLE, 9, InstanceAttrs.empty(), false);
            inventory.getEntries().getFirst().listForMarket();

            assertThatThrownBy(() -> inventory.assertCanAdd(
                    STACKABLE,
                    1,
                    InstanceAttrs.empty(),
                    false
            )).isInstanceOfSatisfying(
                    DomainException.class,
                    exception -> assertThat(exception.getErrorCode())
                            .isEqualTo(InventoryError.INVENTORY_FULL)
            );
        }
    }

    private InventoryEntry entry(int quantity) {
        return entry(quantity, false);
    }

    private InventoryEntry entry(int quantity, boolean bound) {
        PlayerInventory inventory = PlayerInventory.of(294L, 10);
        inventory.add(
                STACKABLE,
                quantity,
                InstanceAttrs.empty(),
                bound
        );
        return inventory.getEntries().getFirst();
    }

    private void assertBoundMarketRestricted(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        DomainException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(InventoryError.BOUND_ENTRY_MARKET_RESTRICTED)
                );
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
