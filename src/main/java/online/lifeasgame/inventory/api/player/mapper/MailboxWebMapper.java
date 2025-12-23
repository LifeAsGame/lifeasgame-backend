package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;

public final class MailboxWebMapper {

    private MailboxWebMapper() {
    }

    public static MailboxResponse.Mails toMails(MailboxResult.Mails result) {
        return new MailboxResponse.Mails(
                result.mails().stream()
                        .map(
                                m -> new MailboxResponse.Mail(
                                        m.slotIndex(),
                                        m.itemId(),
                                        m.rarity(),
                                        m.quantity(),
                                        m.bound()
                                )
                        ).toList()
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
