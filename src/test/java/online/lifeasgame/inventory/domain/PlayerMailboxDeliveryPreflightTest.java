package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerMailbox delivery capacity preflight")
class PlayerMailboxDeliveryPreflightTest {

    @Test
    @DisplayName("신규 stack 공간이 부족하면 기존 stack도 변경하지 않는다")
    void rejectsBeforeMutatingExistingStack() {
        PlayerMailbox mailbox = PlayerMailbox.of(1L, 1);
        ItemCarryPolicy policy = new ItemCarryPolicy(
                10L,
                Rarity.COMMON,
                true,
                10,
                null
        );
        mailbox.deliver(policy, 9, InstanceAttrs.empty(), true);

        MailboxEntry existing = mailbox.getEntries().getFirst();

        assertThatThrownBy(() ->
                mailbox.deliver(policy, 2, InstanceAttrs.empty(), true)
        ).isInstanceOfSatisfying(DomainException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(InventoryError.MAILBOX_FULL)
        );

        assertThat(mailbox.getEntries()).containsExactly(existing);
        assertThat(existing.getQuantity().value()).isEqualTo(9);
    }
}
