package online.lifeasgame.economy.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.player.mapper.EconomyWebMapper;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import online.lifeasgame.economy.api.player.spec.EconomyApiSpecV1;
import online.lifeasgame.economy.application.EconomyFacade;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/economy")
public class EconomyController implements EconomyApiSpecV1 {

    private final EconomyFacade economyFacade;

    @Override
    @PostMapping("/listings")
    public ResponseEntity<ApiResponse<EconomyResponse.ListingId>> openListing(
            @Valid @RequestBody EconomyRequest.OpenListing request
    ) {
        EconomyResult.ListingId id = economyFacade.openListing(EconomyWebMapper.toCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toResponse(id));
    }

    @Override
    @PostMapping("/listings/{listingId}/reserve")
    public ResponseEntity<ApiResponse<EconomyResponse.Reservation>> reserveListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.ReserveListing request
    ) {
        EconomyResult.Reservation reservation = economyFacade.reserveListing(EconomyWebMapper.toCommand(listingId, request));
        return ApiResponses.ok(EconomyWebMapper.toResponse(reservation));
    }

    @Override
    @PostMapping("/listings/{listingId}/purchase")
    public ResponseEntity<ApiResponse<EconomyResponse.Trade>> purchaseListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.PurchaseListing request
    ) {
        EconomyResult.TradeSummary trade = economyFacade.purchaseListing(EconomyWebMapper.toCommand(listingId, request));
        return ApiResponses.ok(EconomyWebMapper.toTrade(trade));
    }

    @Override
    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<ApiResponse<Void>> cancelListing(@PathVariable Long listingId) {
        economyFacade.cancelListing(EconomyWebMapper.toCommand(listingId));
        return ApiResponses.noContent();
    }

    @Override
    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<EconomyResponse.Listings>> listOpenListings() {
        EconomyResult.Listings listings = economyFacade.listOpenListings();
        return ApiResponses.ok(EconomyWebMapper.toResponse(listings));
    }

    @Override
    @PostMapping("/shop/purchase")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseId>> purchaseShopItem(
            @Valid @RequestBody EconomyRequest.PurchaseShopItem request
    ) {
        EconomyResult.ShopPurchaseId id = economyFacade.purchaseShopItem(EconomyWebMapper.toCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toResponse(id));
    }

    @Override
    @PostMapping("/shop/reservations/confirm")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopReservation>> confirmShopReservation(
            @Valid @RequestBody EconomyRequest.ConfirmShopReservation request
    ) {
        EconomyResult.ShopReservation reservation = economyFacade.confirmShopReservation(EconomyWebMapper.toCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toResponse(reservation));
    }

    @Override
    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<Void>> topUp(@Valid @RequestBody EconomyRequest.TopUp request) {
        economyFacade.topUp(EconomyWebMapper.toCommand(request));
        return ApiResponses.noContent();
    }

    @Override
    @GetMapping("/shop/items")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopItems>> listShopItems() {
        EconomyResult.ShopItems items = economyFacade.listShopItems();
        return ApiResponses.ok(EconomyWebMapper.toResponse(items));
    }

    @Override
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<EconomyResponse.WalletBalance>> wallet() {
        EconomyResult.WalletBalance balance = economyFacade.walletBalance();
        return ApiResponses.ok(EconomyWebMapper.toResponse(balance));
    }

    @Override
    @GetMapping("/listings/me")
    public ResponseEntity<ApiResponse<EconomyResponse.PlayerListings>> myListings() {
        EconomyResult.PlayerListings listings = economyFacade.myListings();
        return ApiResponses.ok(EconomyWebMapper.toResponse(listings));
    }

    @Override
    @GetMapping("/listings/reservations")
    public ResponseEntity<ApiResponse<EconomyResponse.PlayerReservations>> myReservations() {
        EconomyResult.PlayerReservations reservations = economyFacade.myReservations();
        return ApiResponses.ok(EconomyWebMapper.toResponse(reservations));
    }

    @Override
    @GetMapping("/shop/purchases")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopPurchases>> myShopPurchases() {
        EconomyResult.ShopPurchases purchases = economyFacade.myShopPurchases();
        return ApiResponses.ok(EconomyWebMapper.toResponse(purchases));
    }

    @Override
    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<EconomyResponse.Trades>> myTrades() {
        EconomyResult.Trades trades = economyFacade.myTrades();
        return ApiResponses.ok(EconomyWebMapper.toResponse(trades));
    }
}
