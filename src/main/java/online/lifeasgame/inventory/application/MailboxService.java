package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.query.MailboxEntryView;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailboxService {

    private final MailboxReader mailboxReader;
    private final InventoryReader inventoryReader;
    private final ItemReader itemReader;
    private final MailBoxQueryReader mailBoxQueryReader;

    @Transactional
    public MailboxResult.Slot deliver(Long playerId, MailboxCommand.Deliver command) {
        Item item = itemReader.getByIdOrThrow(command.itemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);

        PlayerMailbox playerMailbox = mailboxReader.getByPlayerIdOrThrow(playerId);
        SlotIndex slotIndex = playerMailbox.deliver(
                policy,
                command.quantity(),
                InstanceAttrs.of(command.instanceAttrs()),
                command.bound()
        );

        return new MailboxResult.Slot(slotIndex.value());
    }

    @Transactional
    public void claim(Long playerId, MailboxCommand.Claim command) {
        PlayerMailbox mailbox = mailboxReader.getByPlayerIdOrThrow(playerId);
        PlayerInventory inventory = inventoryReader.getByPlayerIdOrThrow(playerId);

        claimSingle(mailbox, inventory, command);
    }

    @Transactional
    public void claimAll(Long playerId, MailboxCommand.ClaimAll command) {
        PlayerMailbox mailbox = mailboxReader.getByPlayerIdOrThrow(playerId);
        PlayerInventory inventory = inventoryReader.getByPlayerIdOrThrow(playerId);

        for (MailboxCommand.Claim claim : command.claims()) {
            claimSingle(mailbox, inventory, claim);
        }
    }

    private void claimSingle(
            PlayerMailbox mailbox,
            PlayerInventory inventory,
            MailboxCommand.Claim command
    ) {
        SlotIndex slotIndex = SlotIndex.of(command.slotIndex());

        MailboxEntry entry = mailbox.getEntry(slotIndex);

        Item item = itemReader.getByIdOrThrow(entry.getItemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);

        PlayerMailbox.ClaimedSlice slice = mailbox.claim(
                slotIndex,
                command.quantity(),
                policy
        );

        inventory.add(
                policy,
                slice.quantity(),
                slice.attrs(),
                slice.bound()
        );
    }
    @Transactional(readOnly = true)
    public MailboxResult.Entries list(Long playerId) {
        List<MailboxEntryView> entryViews = mailBoxQueryReader.list(playerId);
        return MailboxResult.Entries.fromViews(entryViews);
    }

    @Transactional
    public void delete(Long playerId, MailboxCommand.Delete command) {
        PlayerMailbox mailbox = mailboxReader.getByPlayerIdOrThrow(playerId);
        mailbox.deleteEntry(SlotIndex.of(command.slotIndex()));
    }
}
