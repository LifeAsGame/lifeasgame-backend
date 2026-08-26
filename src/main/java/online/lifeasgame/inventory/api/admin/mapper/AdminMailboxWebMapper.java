package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
import online.lifeasgame.inventory.application.result.MailboxResult;
import online.lifeasgame.inventory.api.admin.request.AdminMailboxRequest;
import online.lifeasgame.inventory.api.admin.response.AdminMailboxResponse;

public final class AdminMailboxWebMapper {

    private AdminMailboxWebMapper() {
    }

    public static AdminInventoryEntitlementCommand.DeliverToMailbox toDeliverCommand(
            Long playerId,
            AdminMailboxRequest.Deliver request,
            String idempotencyKey,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.DeliverToMailbox(
                playerId,
                request.itemId(),
                request.quantity(),
                request.bound(),
                request.reason(),
                idempotencyKey,
                correlationId
        );
    }

    public static AdminMailboxResponse.Slot toSlot(MailboxResult.Slot result) {
        return new AdminMailboxResponse.Slot(result.slot());
    }
}
