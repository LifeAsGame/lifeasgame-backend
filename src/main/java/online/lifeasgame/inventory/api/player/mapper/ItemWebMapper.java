package online.lifeasgame.inventory.api.player.mapper;

import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.api.player.response.ItemResponse;

public final class ItemWebMapper {

    private ItemWebMapper() {
    }

    public static ItemResponse.Detail toDetail(ItemResult.Detail result) {
        return new ItemResponse.Detail(
                result.id(),
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

    public static ItemResponse.Summary toSummary(ItemResult.Summary result) {
        return ItemResponse.Summary.of(
                result.id(),
                result.name(),
                result.category(),
                result.type(),
                result.rarity(),
                result.stackable(),
                result.maxStack()
        );
    }

    public static ItemResponse.Page<ItemResponse.Summary> toSummaryPage(ItemResult.Page<ItemResult.Summary> result) {
        return new ItemResponse.Page<>(
                result.content().stream()
                        .map(ItemWebMapper::toSummary)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
