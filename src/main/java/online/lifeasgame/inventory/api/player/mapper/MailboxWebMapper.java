package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.player.request.MailboxRequest;
import online.lifeasgame.inventory.api.player.response.MailboxResponse;

public final class MailboxWebMapper {
    private MailboxWebMapper() {
    }

    public static MailboxCommand.Deliver toCommand(MailboxRequest.Deliver request) {
        return MailboxCommand.Deliver.of(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static MailboxCommand.Claim toCommand(MailboxRequest.Claim request) {
        return MailboxCommand.Claim.of(request.slotIndex(), request.quantity(), request.itemId());
    }

    public static MailboxResponse.Mails toLMails(MailboxResult.Mails result) {
        return MailboxResponse.Mails.of(
                result.mails().stream()
                        .map(
                                m -> MailboxResponse.Mail.of(
                                        m.slotIndex(),
                                        m.itemId(),
                                        m.rarity(),
                                        m.quantity(),
                                        m.bound()
                                )
                        ).toList()
        );
    }
}
