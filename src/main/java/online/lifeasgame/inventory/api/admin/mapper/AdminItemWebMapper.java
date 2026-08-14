package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;

public final class AdminItemWebMapper {

    private AdminItemWebMapper(){}

    public static AdminItemResponse.Id toInfo(ItemResult.Id result){
        return new AdminItemResponse.Id(result.id());
    }

    public static ItemCommand.Create toCreateCommand(AdminItemRequest.Create request) {
        return new ItemCommand.Create(
                request.name(),
                request.category(),
                request.type(),
                request.equipmentCompatibilityKind(),
                request.rarity(),
                request.baseAttrs(),
                request.stackable(),
                request.maxStack(),
                request.maxDurability()
        );
    }

    public static ItemCommand.Update toUpdateCommand(Long itemId, AdminItemRequest.Update request) {
        return new ItemCommand.Update(
                itemId,
                request.name(),
                request.category(),
                request.type(),
                request.equipmentCompatibilityKind(),
                request.rarity(),
                request.baseAttrs(),
                request.stackable(),
                request.maxStack(),
                request.maxDurability()
        );
    }

    public static AdminItemResponse.Deleted toDeleted(ItemResult.Deleted result){
        return new AdminItemResponse.Deleted(result.id());
    }
}
