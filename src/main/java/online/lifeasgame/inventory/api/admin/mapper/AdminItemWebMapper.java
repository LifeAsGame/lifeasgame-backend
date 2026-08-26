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

    public static AdminItemResponse.Detail toDetail(ItemResult.Detail result) {
        return new AdminItemResponse.Detail(
                result.id(),
                result.code(),
                result.name(),
                result.category(),
                result.type(),
                result.rarity(),
                result.stackable(),
                result.maxStack(),
                result.maxDurability(),
                result.baseAttrs()
        );
    }

    public static AdminItemResponse.Page<AdminItemResponse.Summary> toSummaryPage(
            ItemResult.Page<ItemResult.Summary> result
    ) {
        return new AdminItemResponse.Page<>(
                result.content().stream()
                        .map(AdminItemWebMapper::toSummary)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private static AdminItemResponse.Summary toSummary(ItemResult.Summary result) {
        return new AdminItemResponse.Summary(
                result.id(),
                result.code(),
                result.name(),
                result.category(),
                result.type(),
                result.rarity(),
                result.stackable(),
                result.maxStack()
        );
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
