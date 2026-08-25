package online.lifeasgame.economy.api.admin.mapper;

import online.lifeasgame.economy.api.admin.request.AdminEconomyRequest;
import online.lifeasgame.economy.api.admin.response.AdminEconomyResponse;
import online.lifeasgame.economy.application.command.AdminWalletAdjustmentCommand;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;

public final class AdminEconomyWebMapper {

    private AdminEconomyWebMapper() {
    }

    public static AdminEconomyResponse.ShopItems toShopItems(EconomyResult.ShopItems result) {
        return new AdminEconomyResponse.ShopItems(
                result.items().stream()
                        .map(AdminEconomyWebMapper::toShopItem)
                        .toList()
        );
    }

    public static EconomyCommand.CreateShopItem toCreateShopItemCommand(AdminEconomyRequest.CreateShopItem request) {
        return new EconomyCommand.CreateShopItem(
                request.itemId(),
                request.price(),
                request.currency(),
                request.globalLimit(),
                request.perPlayerLimit(),
                request.reservationTtlSeconds()
        );
    }

    public static EconomyCommand.UpdateShopItem toUpdateShopItemCommand(
            Long shopItemId,
            AdminEconomyRequest.UpdateShopItem request
    ) {
        return new EconomyCommand.UpdateShopItem(
                shopItemId,
                request.globalLimit(),
                request.perPlayerLimit(),
                request.reservationTtlSeconds()
        );
    }

    public static AdminEconomyResponse.ShopItem toShopItem(EconomyResult.ShopItemView result) {
        return new AdminEconomyResponse.ShopItem(
                result.id(),
                result.itemId(),
                result.price(),
                result.currency(),
                result.available(),
                result.globalStockLimit(),
                result.perPlayerLimit(),
                result.reservationTtlSec()
        );
    }

    public static EconomyCommand.ToggleShopItem toToggleShopItemCommand(
            Long shopItemId,
            AdminEconomyRequest.ToggleShopItem request
    ) {
        return new EconomyCommand.ToggleShopItem(shopItemId, request.enabled());
    }

    public static AdminEconomyResponse.ShopPurchases toShopPurchases(EconomyResult.ShopPurchases result) {
        return new AdminEconomyResponse.ShopPurchases(
                result.purchases().stream()
                        .map(AdminEconomyWebMapper::toShopPurchaseSummary)
                        .toList()
        );
    }

    public static AdminEconomyResponse.ShopPurchaseSummary toShopPurchaseSummary(EconomyResult.ShopPurchaseView result) {
        return new AdminEconomyResponse.ShopPurchaseSummary(
                result.id(),
                result.shopItemId(),
                result.playerId(),
                result.quantity(),
                result.status(),
                result.reservationToken(),
                result.reservationExpiresAt()
        );
    }

    public static AdminWalletAdjustmentCommand toAdjustWalletCommand(
            Long playerId,
            AdminEconomyRequest.AdjustWallet request,
            String idempotencyKey,
            String correlationId
    ) {
        return new AdminWalletAdjustmentCommand(
                playerId,
                request.amount(),
                request.currency(),
                request.debit(),
                request.reason(),
                idempotencyKey,
                correlationId
        );
    }

    public static AdminEconomyResponse.WalletBalance toWalletBalance(EconomyResult.WalletBalance result) {
        return new AdminEconomyResponse.WalletBalance(result.amount(), result.currency());
    }
}
