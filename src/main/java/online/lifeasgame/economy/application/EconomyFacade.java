package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.application.command.EconomyCommand;
import online.lifeasgame.economy.application.result.EconomyResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EconomyFacade {

    private final MarketplaceService marketplaceService;
    private final ShopService shopService;
    private final TopUpService topUpService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public EconomyResult.ListingId openListing(EconomyCommand.OpenListing command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.open(playerId, command);
    }

    public EconomyResult.Reservation reserveListing(EconomyCommand.ReserveListing command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.reserve(playerId, command);
    }

    public EconomyResult.TradeSummary purchaseListing(EconomyCommand.PurchaseListing command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.purchase(playerId, command);
    }

    public void cancelListing(EconomyCommand.CancelListing command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        marketplaceService.cancel(playerId, command);
    }

    public EconomyResult.ShopPurchaseId purchaseShopItem(EconomyCommand.PurchaseShopItem command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return shopService.purchase(playerId, command);
    }

    public EconomyResult.ShopReservation confirmShopReservation(EconomyCommand.ConfirmShopReservation command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return shopService.confirmReservation(playerId, command);
    }

    public void topUp(EconomyCommand.TopUp command) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        topUpService.topUp(playerId, command);
    }

    public EconomyResult.Listings listOpenListings() {
        return marketplaceService.listOpen();
    }

    public EconomyResult.ShopItems listShopItems() {
        return shopService.listAvailableItems();
    }

    public EconomyResult.WalletBalance walletBalance() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return topUpService.wallet(playerId);
    }

    public EconomyResult.PlayerListings myListings() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.listBySeller(playerId);
    }

    public EconomyResult.PlayerReservations myReservations() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.listReservations(playerId);
    }

    public EconomyResult.ShopPurchases myShopPurchases() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return shopService.listPurchases(playerId);
    }

    public EconomyResult.Trades myTrades() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return marketplaceService.listTrades(playerId);
    }
}
