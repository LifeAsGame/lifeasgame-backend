package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerMailbox claim plan")
class PlayerMailboxClaimPlanTest {

    private static final ItemCarryPolicy POLICY = new ItemCarryPolicy(
            10L,
            Rarity.RARE,
            true,
            10,
            null
    );

    @Test
    @DisplayName("plan은 attrs와 bound를 snapshot하고 apply 전에는 변경하지 않는다")
    void plansThenAppliesClaim() {
        PlayerMailbox mailbox = PlayerMailbox.of(1L, 2);
        InstanceAttrs attrs = InstanceAttrs.of(Map.of("grade", "A"));
        SlotIndex slot = mailbox.deliver(POLICY, 3, attrs, true);

        PlayerMailbox.ClaimPlan plan = mailbox.planClaim(slot, 2, POLICY);

        assertThat(plan.itemId()).isEqualTo(POLICY.itemId());
        assertThat(plan.quantity()).isEqualTo(2);
        assertThat(plan.attrs()).isEqualTo(attrs);
        assertThat(plan.bound()).isTrue();
        assertThat(mailbox.getEntryOrThrow(slot).getQuantity().value())
                .isEqualTo(3);

        mailbox.applyClaim(plan);

        assertThat(mailbox.getEntryOrThrow(slot).getQuantity().value())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("빈 slot은 nullable 대신 stable SLOT_EMPTY다")
    void rejectsEmptySlot() {
        PlayerMailbox mailbox = PlayerMailbox.of(1L, 2);

        assertError(
                () -> mailbox.getEntryOrThrow(SlotIndex.of(0)),
                InventoryError.SLOT_EMPTY
        );
        assertError(
                () -> mailbox.planClaim(SlotIndex.of(0), 1, POLICY),
                InventoryError.SLOT_EMPTY
        );
    }

    @Test
    @DisplayName("invalid quantity와 policy mismatch는 Mailbox를 변경하지 않는다")
    void rejectsInvalidPlanWithoutMutation() {
        PlayerMailbox mailbox = PlayerMailbox.of(1L, 2);
        SlotIndex slot = mailbox.deliver(
                POLICY,
                2,
                InstanceAttrs.empty(),
                false
        );
        ItemCarryPolicy otherPolicy = new ItemCarryPolicy(
                11L,
                Rarity.COMMON,
                true,
                10,
                null
        );

        assertError(
                () -> mailbox.planClaim(slot, 0, POLICY),
                InventoryError.INVALID_QUANTITY
        );
        assertError(
                () -> mailbox.planClaim(slot, 3, POLICY),
                InventoryError.NOT_ENOUGH_QUANTITY
        );
        assertError(
                () -> mailbox.planClaim(slot, 1, otherPolicy),
                InventoryError.ITEM_NOT_FOUND
        );

        assertThat(mailbox.getEntryOrThrow(slot).getQuantity().value())
                .isEqualTo(2);
    }

    private void assertError(Runnable call, InventoryError error) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
