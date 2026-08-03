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

    private static MailboxResponse.Entry toEntry(MailboxResult.Entry result) {
        return new MailboxResponse.Entry(
                result.mailId(),
                result.slotIndex(),
                result.itemId(),
                result.itemName(),
                result.category(),
                result.type(),
                result.rarity(),
                result.stackable(),
                result.maxStack(),
                result.quantity(),
                result.bound(),
                result.durability(),
                result.instanceAttrs()
        );
    }

    public static MailboxCommand.Claim toClaimCommand(MailboxRequest.Claim request) {
        return new MailboxCommand.Claim(request.slotIndex(), request.quantity());
    }

    public static MailboxCommand.ClaimAll toClaimAllCommand(MailboxRequest.ClaimAll request) {
        return new MailboxCommand.ClaimAll(
                request.claims().stream()
                        .map(
                                claim -> new MailboxCommand.Claim(
                                        claim.slotIndex(),
                                        claim.quantity()
                                )
                        )
                        .toList()
        );
    }

    public static MailboxCommand.Delete toDeleteCommand(MailboxRequest.Delete request) {
        return new MailboxCommand.Delete(request.slotIndex());
    }
}
