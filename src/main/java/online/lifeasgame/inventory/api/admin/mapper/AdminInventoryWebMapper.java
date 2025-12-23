package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.response.AdminInventoryResponse;

public final class AdminInventoryWebMapper {

    private AdminInventoryWebMapper() {
    }

    public static InventoryCommand.Add toAddCommand(AdminInventoryRequest.Add request) {
        return new InventoryCommand.Add(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static AdminInventoryResponse.Slots toSlots(InventoryResult.Slots result) {
        return new AdminInventoryResponse.Slots(result.slots());
    }
}
