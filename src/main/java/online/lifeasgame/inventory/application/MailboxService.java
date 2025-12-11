package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.command.MailboxCommand;
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
        SlotIndex slotIndex = SlotIndex.of(command.slotIndex());

        PlayerMailbox playerMailbox = mailboxReader.getByPlayerIdOrThrow(playerId);
        MailboxEntry mailboxEntry = playerMailbox.getEntry(slotIndex);

        Item item = itemReader.getByIdOrThrow(mailboxEntry.getItemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);

        PlayerMailbox.ClaimedSlice slice = playerMailbox.claim(
                SlotIndex.of(command.slotIndex()),
                command.quantity(),
                policy
        );

        PlayerInventory playerInventory = inventoryReader.getByPlayerIdOrThrow(playerId);
        playerInventory.add(
                policy,
                slice.quantity(),
                slice.attrs(),
                slice.bound()
        );
    }

    @Transactional(readOnly = true)
    public MailboxResult.Mails list(Long playerId) {
        List<MailboxEntry> mailboxEntries = mailboxReader.getByPlayerIdOrThrow(playerId).getEntries();
        return MailboxResult.Mails.from(mailboxEntries);
    }
}
