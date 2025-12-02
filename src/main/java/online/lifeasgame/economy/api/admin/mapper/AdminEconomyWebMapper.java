package online.lifeasgame.economy.api.admin.mapper;

import online.lifeasgame.economy.api.admin.request.AdminEconomyRequest;
import online.lifeasgame.economy.api.admin.response.AdminEconomyResponse;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.domain.Currency;

public final class AdminEconomyWebMapper {
    private AdminEconomyWebMapper() {}

    public static EconomyCommand.CreateShopItem toCommand(AdminEconomyRequest.CreateShopItem request) {
        Currency currency = Currency.parseOptional(request.currency(), Currency.GOLD);
        return EconomyCommand.CreateShopItem.of(
                request.itemId(),
                request.price(),
                currency,
                request.globalLimit(),
                request.perPlayerLimit(),
                request.reservationTtlSeconds()
        );
    }

    public static EconomyCommand.ToggleShopItem toCommand(Long shopItemId, AdminEconomyRequest.ToggleShopItem request) {
        return EconomyCommand.ToggleShopItem.of(shopItemId, request.enabled());
    }

    public static EconomyCommand.UpdateShopItem toCommand(Long shopItemId, AdminEconomyRequest.UpdateShopItem request) {
        return EconomyCommand.UpdateShopItem.of(shopItemId, request.globalLimit(), request.perPlayerLimit(), request.reservationTtlSeconds());
    }

    public static EconomyCommand.AdjustWallet toCommand(Long playerId, AdminEconomyRequest.AdjustWallet request) {
        Currency currency = Currency.parseOptional(request.currency(), Currency.GOLD);
        return EconomyCommand.AdjustWallet.of(playerId, request.amount(), currency, request.debit(), request.reason());
    }

    public static AdminEconomyResponse.ShopItem toResponse(EconomyResult.ShopItemView view) {
        return new AdminEconomyResponse.ShopItem(
                view.id(),
                view.itemId(),
                view.price(),
                view.currency(),
                view.available(),
                view.globalStockLimit(),
                view.perPlayerLimit(),
                view.reservationTtlSec()
        );
    }

    public static AdminEconomyResponse.ShopItems toResponse(EconomyResult.ShopItems items) {
        return new AdminEconomyResponse.ShopItems(items.items().stream().map(AdminEconomyWebMapper::toResponse).toList());
    }

    public static AdminEconomyResponse.ShopPurchaseSummary toResponse(EconomyResult.ShopPurchaseView view) {
        return new AdminEconomyResponse.ShopPurchaseSummary(
                view.id(),
                view.shopItemId(),
                view.playerId(),
                view.quantity(),
                view.status(),
                view.reservationToken(),
                view.reservationExpiresAt()
        );
    }

    public static AdminEconomyResponse.ShopPurchases toResponse(EconomyResult.ShopPurchases purchases) {
        return new AdminEconomyResponse.ShopPurchases(purchases.purchases().stream()
                .map(AdminEconomyWebMapper::toResponse)
                .toList()
        );
    }

    public static AdminEconomyResponse.WalletBalance toResponse(EconomyResult.WalletBalance balance) {
        return new AdminEconomyResponse.WalletBalance(balance.amount(), balance.currency());
    }
}
