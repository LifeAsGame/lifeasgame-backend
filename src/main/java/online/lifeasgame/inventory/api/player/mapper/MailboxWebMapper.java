package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;
import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;

public final class MailboxWebMapper {

    private MailboxWebMapper() {
    }

    public static MailboxResponse.Entries toMails(MailboxResult.Entries result) {
        return new MailboxResponse.Entries(
                result.entries().stream()
                        .map(MailboxWebMapper::toEntry)
                        .toList()
        );
    }

    private static MailboxResponse.Entry toEntry(MailboxResult.Entry e) {
        return new MailboxResponse.Entry(
                e.mailId(),
                e.slotIndex(),
                e.itemId(),
                e.itemName(),
                e.category(),
                e.type(),
                e.rarity(),
                e.stackable(),
                e.maxStack(),
                e.quantity(),
                e.bound(),
                e.durability(),
                e.instanceAttrs()
        );
    }

    public static MailboxCommand.Deliver toDeliverCommand(MailboxRequest.Deliver request) {
        return new MailboxCommand.Deliver(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static MailboxResponse.Slot toSlot(MailboxResult.Slot slot) {
        return new MailboxResponse.Slot(slot.slot());
    }

    public static MailboxCommand.Claim toClaimCommand(MailboxRequest.Claim request) {
        return new MailboxCommand.Claim(request.slotIndex(), request.quantity());
    }
}
