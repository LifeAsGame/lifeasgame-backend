package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.InventoryCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.reseponse.AdminInventoryResponse;

public final class AdminInventoryWebMapper {

    private AdminInventoryWebMapper() {
    }

    public static InventoryCommand.Add toCommand(AdminInventoryRequest.Add request) {
        return InventoryCommand.Add.of(
                request.itemId(),
                request.quantity(),
                request.instanceAttrs(),
                request.bound()
        );
    }

    public static AdminInventoryResponse.Slots toSlots(InventoryResult.Slots r) {
        return AdminInventoryResponse.Slots.of(r.slots());
    }
}
