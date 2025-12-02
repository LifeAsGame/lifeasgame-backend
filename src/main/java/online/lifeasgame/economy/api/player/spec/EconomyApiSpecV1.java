package online.lifeasgame.economy.api.player.spec;

import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import org.springframework.http.ResponseEntity;

public interface EconomyApiSpecV1 {
    ResponseEntity<ApiResponse<EconomyResponse.ListingId>> openListing(EconomyRequest.OpenListing request);
    ResponseEntity<ApiResponse<EconomyResponse.Reservation>> reserveListing(Long listingId, EconomyRequest.ReserveListing request);
    ResponseEntity<ApiResponse<EconomyResponse.Trade>> purchaseListing(Long listingId, EconomyRequest.PurchaseListing request);
    ResponseEntity<ApiResponse<Void>> cancelListing(Long listingId);
    ResponseEntity<ApiResponse<EconomyResponse.Listings>> listOpenListings();
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseId>> purchaseShopItem(EconomyRequest.PurchaseShopItem request);
    ResponseEntity<ApiResponse<EconomyResponse.ShopReservation>> confirmShopReservation(EconomyRequest.ConfirmShopReservation request);
    ResponseEntity<ApiResponse<Void>> topUp(EconomyRequest.TopUp request);
    ResponseEntity<ApiResponse<EconomyResponse.ShopItems>> listShopItems();
    ResponseEntity<ApiResponse<EconomyResponse.WalletBalance>> wallet();
    ResponseEntity<ApiResponse<EconomyResponse.PlayerListings>> myListings();
    ResponseEntity<ApiResponse<EconomyResponse.PlayerReservations>> myReservations();
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchases>> myShopPurchases();
    ResponseEntity<ApiResponse<EconomyResponse.Trades>> myTrades();
}
