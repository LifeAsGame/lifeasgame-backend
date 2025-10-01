package online.lifeasgame.inventory.api.admin.mapper;

import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.api.admin.request.AdminItemRequest;
import online.lifeasgame.inventory.api.admin.response.AdminItemResponse;

public final class AdminItemWebMapper {

    private AdminItemWebMapper(){}

    public static ItemCommand.Create toCommand(AdminItemRequest.Create r) {
        return ItemCommand.Create.of(
                r.name(),
                r.category(),
                r.type(),
                r.rarity(),
                r.baseAttrs(),
                r.stackable(),
                r.maxStack(),
                r.maxDurability()
        );
    }

    public static ItemCommand.Update toCommand(Long itemId, AdminItemRequest.Update r) {
        return ItemCommand.Update.of(
                itemId,
                r.name(),
                r.category(),
                r.type(),
                r.rarity(),
                r.baseAttrs(),
                r.stackable(),
                r.maxStack(),
                r.maxDurability()
        );
    }

    public static AdminItemResponse.Id toResponse(ItemResult.Id r){
        return AdminItemResponse.Id.of(r.id());
    }
    public static AdminItemResponse.Deleted toResponse(ItemResult.Deleted r){
        return AdminItemResponse.Deleted.of(r.id());
    }

}
