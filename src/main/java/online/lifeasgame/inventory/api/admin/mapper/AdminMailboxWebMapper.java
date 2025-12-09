package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.MailboxCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.response.AdminMailboxResponse;

public final class AdminMailboxWebMapper {

    private AdminMailboxWebMapper() {
    }

    public static MailboxCommand.Deliver toCommand(AdminMailboxRequest.Deliver request) {
        return new MailboxCommand.Deliver(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static AdminMailboxResponse.Slot toSlot(MailboxResult.Slot result) {
        return new AdminMailboxResponse.Slot(result.slot());
    }
}
