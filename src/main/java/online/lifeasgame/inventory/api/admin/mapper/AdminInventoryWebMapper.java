package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.AdminInventoryEntitlementCommand;
import online.lifeasgame.inventory.application.result.InventoryResult;
import online.lifeasgame.inventory.api.admin.request.AdminInventoryRequest;
import online.lifeasgame.inventory.api.admin.response.AdminInventoryResponse;

public final class AdminInventoryWebMapper {

    private AdminInventoryWebMapper() {
    }

    public static AdminInventoryEntitlementCommand.AddToInventory toAddCommand(
            Long playerId,
            AdminInventoryRequest.Add request,
            String idempotencyKey,
            String correlationId
    ) {
        return new AdminInventoryEntitlementCommand.AddToInventory(
                playerId,
                request.itemId(),
                request.quantity(),
                request.bound(),
                request.reason(),
                idempotencyKey,
                correlationId
        );
    }

    public static AdminInventoryResponse.Slots toSlots(InventoryResult.Slots result) {
        return new AdminInventoryResponse.Slots(result.slots());
    }

    public static AdminInventoryResponse.Entries toEntries(
            Long playerId,
            InventoryResult.Entries result
    ) {
        return new AdminInventoryResponse.Entries(
                playerId,
                result.entryViews().stream()
                        .map(AdminInventoryWebMapper::toEntry)
                        .toList()
        );
    }

    private static AdminInventoryResponse.Entry toEntry(InventoryResult.Entry result) {
        return new AdminInventoryResponse.Entry(
                result.itemInstanceId(),
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
                result.durability()
        );
    }
}
