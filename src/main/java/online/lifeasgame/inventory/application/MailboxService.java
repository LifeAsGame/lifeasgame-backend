package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.model.InventorySpec;
import online.lifeasgame.inventory.application.model.MailboxSpec;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MailboxService {

    private final MailboxReader mailboxReader;
    private final MailboxWriter mailboxWriter;
    private final InventoryWriter inventoryWriter;
    private final InventoryReader inventoryReader;
    private final ItemReader itemReader;

    @Transactional
    public MailboxResult.Slot deliver(Long playerId, MailboxCommand.Deliver command) {
        Item item = itemReader.getItem(command.itemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);
        PlayerMailbox playerMailbox = mailboxReader.getPlayerMailbox(playerId);

        SlotIndex slotIndex = mailboxWriter.deliver(playerMailbox, policy, MailboxSpec.Deliver.from(command));
        return MailboxResult.Slot.of(slotIndex.value());
    }

    @Transactional
    public void claim(Long playerId, MailboxCommand.Claim command) {

        Item item = itemReader.getItem(command.itemId());
        ItemCarryPolicy policy = ItemCarryPolicy.from(item);
        PlayerMailbox playerMailbox = mailboxReader.getPlayerMailbox(playerId);

        var slice = mailboxWriter.claimSlice(playerMailbox, policy, MailboxSpec.Claim.from(command));

        PlayerInventory playerInventory = inventoryReader.getPlayerInventory(playerId);

        inventoryWriter.add(
                playerInventory,
                policy,
                InventorySpec.Add.of(slice.quantity(), slice.attrs(), slice.bound())
        );
    }

    @Transactional(readOnly = true)
    public MailboxResult.Mails list(Long playerId) {
        List<MailboxEntry> mailboxEntries = mailboxReader.getPlayerMailbox(playerId).getEntries();
        return MailboxResult.Mails.from(mailboxEntries);
    }
}
