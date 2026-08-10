package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.domain.*;
import online.lifeasgame.inventory.domain.error.InventoryError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MailboxService {

    private final MailboxReader mailboxReader;
    private final InventoryReader inventoryReader;
    private final ItemReader itemReader;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Transactional
    public MailboxResult.Slot deliver(Long playerId, MailboxCommand.Deliver command) {
        Item item = itemReader.getByIdOrThrow(command.itemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);

        PlayerMailbox playerMailbox = mailboxReader
                .getByPlayerIdForUpdateOrThrow(playerId);
        SlotIndex slotIndex = playerMailbox.deliver(
                policy,
                command.quantity(),
                InstanceAttrs.of(command.instanceAttrs()),
                command.bound()
        );

        return new MailboxResult.Slot(slotIndex.value());
    }

    @Transactional
    public void claim(MailboxCommand.Claim command) {
        claim(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public void claim(Long playerId, MailboxCommand.Claim command) {
        validateClaim(command);
        PlayerMailbox mailbox = mailboxReader
                .getByPlayerIdForUpdateOrThrow(playerId);
        PlayerInventory inventory = inventoryReader
                .getByPlayerIdForUpdateOrThrow(playerId);

        claim(mailbox, inventory, List.of(command));
    }

    @Transactional
    public void claimAll(MailboxCommand.ClaimAll command) {
        claimAll(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public void claimAll(Long playerId, MailboxCommand.ClaimAll command) {
        List<MailboxCommand.Claim> claims = validateClaimAll(command);
        PlayerMailbox mailbox = mailboxReader
                .getByPlayerIdForUpdateOrThrow(playerId);
        PlayerInventory inventory = inventoryReader
                .getByPlayerIdForUpdateOrThrow(playerId);

        claim(mailbox, inventory, claims);
    }

    private void claim(
            PlayerMailbox mailbox,
            PlayerInventory inventory,
            List<MailboxCommand.Claim> claims
    ) {
        List<PlayerMailbox.ClaimPlan> plans = claims.stream()
                .map(command -> planClaim(mailbox, command))
                .toList();
        List<PlayerInventory.Addition> additions = plans.stream()
                .map(plan -> new PlayerInventory.Addition(
                        plan.policy(),
                        plan.quantity(),
                        plan.attrs(),
                        plan.bound()
                ))
                .toList();

        inventory.assertCanAddAll(additions);
        plans.forEach(mailbox::applyClaim);
        additions.forEach(addition -> inventory.add(
                addition.policy(),
                addition.quantity(),
                addition.attrs(),
                addition.bound()
        ));
    }

    private PlayerMailbox.ClaimPlan planClaim(
            PlayerMailbox mailbox,
            MailboxCommand.Claim command
    ) {
        SlotIndex slot = SlotIndex.of(command.slotIndex());
        MailboxEntry entry = mailbox.getEntryOrThrow(slot);
        Item item = itemReader.getByIdOrThrow(entry.getItemId());
        return mailbox.planClaim(
                slot,
                command.quantity(),
                ItemCarryPolicy.from(item)
        );
    }

    private List<MailboxCommand.Claim> validateClaimAll(
            MailboxCommand.ClaimAll command
    ) {
        if (command == null
                || command.claims() == null
                || command.claims().isEmpty()
                || command.claims().stream().anyMatch(java.util.Objects::isNull)) {
            throw new DomainException(InventoryError.MAILBOX_CLAIM_EMPTY);
        }
        if (command.claims().size() > PlayerMailbox.DEFAULT_CAPACITY) {
            throw new DomainException(InventoryError.MAILBOX_CLAIM_TOO_LARGE);
        }

        Set<Integer> slots = new HashSet<>();
        for (MailboxCommand.Claim claim : command.claims()) {
            validateClaim(claim);
            if (!slots.add(claim.slotIndex())) {
                throw new DomainException(
                        InventoryError.MAILBOX_CLAIM_DUPLICATE_SLOT
                );
            }
        }
        return command.claims();
    }

    private void validateClaim(MailboxCommand.Claim command) {
        if (command == null) {
            throw new DomainException(InventoryError.MAILBOX_CLAIM_EMPTY);
        }
        if (command.slotIndex() < 0) {
            throw new DomainException(InventoryError.INVALID_SLOT);
        }
        if (command.quantity() < 1) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }
    }

    @Transactional
    public void delete(MailboxCommand.Delete command) {
        delete(currentPlayerAccessor.currentPlayerIdOrThrow(), command);
    }

    @Transactional
    public void delete(Long playerId, MailboxCommand.Delete command) {
        PlayerMailbox mailbox = mailboxReader
                .getByPlayerIdForUpdateOrThrow(playerId);
        mailbox.deleteEntry(SlotIndex.of(command.slotIndex()));
    }
}
