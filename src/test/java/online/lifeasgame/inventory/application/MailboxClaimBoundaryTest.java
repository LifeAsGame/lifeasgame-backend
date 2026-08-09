package online.lifeasgame.inventory.application;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.domain.*;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mailbox claim boundary")
class MailboxClaimBoundaryTest {

    private static final Long PLAYER_ID = 2280L;
    private static final Long ITEM_ID = 228L;

    @Mock
    private MailboxReader mailboxReader;

    @Mock
    private InventoryReader inventoryReader;

    @Mock
    private ItemReader itemReader;

    @Mock
    private CurrentPlayerAccessor currentPlayerAccessor;

    private MailboxService service;

    @BeforeEach
    void setUp() {
        service = new MailboxService(
                mailboxReader,
                inventoryReader,
                itemReader,
                currentPlayerAccessor
        );
    }

    @Test
    @DisplayName("null, empty, null element, size 초과를 lock 전에 거부한다")
    void rejectsEmptyAndOversizedBatch() {
        assertError(
                () -> service.claimAll(PLAYER_ID, null),
                InventoryError.MAILBOX_CLAIM_EMPTY
        );
        assertError(
                () -> service.claimAll(
                        PLAYER_ID,
                        new MailboxCommand.ClaimAll(null)
                ),
                InventoryError.MAILBOX_CLAIM_EMPTY
        );
        assertError(
                () -> service.claimAll(
                        PLAYER_ID,
                        new MailboxCommand.ClaimAll(List.of())
                ),
                InventoryError.MAILBOX_CLAIM_EMPTY
        );
        assertError(
                () -> service.claimAll(
                        PLAYER_ID,
                        new MailboxCommand.ClaimAll(
                                Collections.singletonList(null)
                        )
                ),
                InventoryError.MAILBOX_CLAIM_EMPTY
        );
        assertError(
                () -> service.claimAll(
                        PLAYER_ID,
                        new MailboxCommand.ClaimAll(
                                new ArrayList<>(Collections.nCopies(
                                        PlayerMailbox.DEFAULT_CAPACITY + 1,
                                        new MailboxCommand.Claim(0, 1)
                                ))
                        )
                ),
                InventoryError.MAILBOX_CLAIM_TOO_LARGE
        );

        verifyNoInteractions(mailboxReader, inventoryReader, itemReader);
    }

    @Test
    @DisplayName("duplicate slot과 invalid slot/quantity를 stable error로 거부한다")
    void rejectsInvalidClaims() {
        assertError(
                () -> service.claimAll(
                        PLAYER_ID,
                        new MailboxCommand.ClaimAll(List.of(
                                new MailboxCommand.Claim(0, 1),
                                new MailboxCommand.Claim(0, 1)
                        ))
                ),
                InventoryError.MAILBOX_CLAIM_DUPLICATE_SLOT
        );
        assertError(
                () -> service.claim(
                        PLAYER_ID,
                        new MailboxCommand.Claim(-1, 1)
                ),
                InventoryError.INVALID_SLOT
        );
        assertError(
                () -> service.claim(
                        PLAYER_ID,
                        new MailboxCommand.Claim(0, 0)
                ),
                InventoryError.INVALID_QUANTITY
        );

        verifyNoInteractions(mailboxReader, inventoryReader, itemReader);
    }

    @Test
    @DisplayName("claim은 Mailbox 후 Inventory를 잠그고 snapshot을 그대로 옮긴다")
    void locksInCanonicalOrderAndClaims() {
        Item item = item();
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);
        PlayerMailbox mailbox = PlayerMailbox.of(PLAYER_ID, 2);
        mailbox.deliver(policy, 1, InstanceAttrs.empty(), true);
        PlayerInventory inventory = PlayerInventory.of(PLAYER_ID, 2);
        given(mailboxReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID))
                .willReturn(mailbox);
        given(inventoryReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID))
                .willReturn(inventory);
        given(itemReader.getByIdOrThrow(ITEM_ID)).willReturn(item);

        service.claim(PLAYER_ID, new MailboxCommand.Claim(0, 1));

        InOrder locks = inOrder(mailboxReader, inventoryReader);
        locks.verify(mailboxReader).getByPlayerIdForUpdateOrThrow(PLAYER_ID);
        locks.verify(inventoryReader).getByPlayerIdForUpdateOrThrow(PLAYER_ID);
        verify(mailboxReader, never()).getByPlayerIdOrThrow(anyLong());
        verify(inventoryReader, never()).getByPlayerIdOrThrow(anyLong());
        assertThat(mailbox.getEntries()).isEmpty();
        assertThat(inventory.getEntries()).hasSize(1);
        assertThat(inventory.getEntries().getFirst().isBound()).isTrue();
    }

    @Test
    @DisplayName("deliver와 delete도 Mailbox for-update 조회를 사용한다")
    void locksDeliverAndDelete() {
        Item item = item();
        PlayerMailbox mailbox = PlayerMailbox.of(PLAYER_ID, 2);
        given(itemReader.getByIdOrThrow(ITEM_ID)).willReturn(item);
        given(mailboxReader.getByPlayerIdForUpdateOrThrow(PLAYER_ID))
                .willReturn(mailbox);

        service.deliver(
                PLAYER_ID,
                new MailboxCommand.Deliver(ITEM_ID, 1, null, false)
        );
        service.delete(PLAYER_ID, new MailboxCommand.Delete(0));

        verify(mailboxReader, times(2))
                .getByPlayerIdForUpdateOrThrow(PLAYER_ID);
        verify(mailboxReader, never()).getByPlayerIdOrThrow(anyLong());
        assertThat(mailbox.getEntries()).isEmpty();
    }

    @Test
    @DisplayName("ClaimAll API는 collection과 element validation을 선언한다")
    void validatesApiRequest() {
        Validator validator = Validation.buildDefaultValidatorFactory()
                .getValidator();

        assertThat(validator.validate(new MailboxRequest.ClaimAll(null)))
                .isNotEmpty();
        assertThat(validator.validate(new MailboxRequest.ClaimAll(List.of())))
                .isNotEmpty();
        assertThat(validator.validate(new MailboxRequest.ClaimAll(
                Collections.singletonList(null)
        ))).isNotEmpty();
        assertThat(validator.validate(new MailboxRequest.ClaimAll(List.of(
                new MailboxRequest.Claim(0, 0)
        )))).isNotEmpty();
    }

    private Item item() {
        Item item = mock(Item.class);
        given(item.getId()).willReturn(ITEM_ID);
        given(item.getRarity()).willReturn(Rarity.COMMON);
        given(item.isStackable()).willReturn(true);
        given(item.maxStack()).willReturn(10);
        return item;
    }

    private void assertError(Runnable call, InventoryError error) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(error)
                );
    }
}
