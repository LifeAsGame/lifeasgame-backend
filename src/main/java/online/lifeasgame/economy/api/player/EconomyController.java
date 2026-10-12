package online.lifeasgame.economy.api.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.economy.api.player.mapper.EconomyWebMapper;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import online.lifeasgame.economy.api.player.spec.EconomyApiSpecV1;
import online.lifeasgame.economy.application.ListingOpenService;
import online.lifeasgame.economy.application.MarketplaceService;
import online.lifeasgame.economy.application.ShopService;
import online.lifeasgame.economy.application.TopUpService;
import online.lifeasgame.economy.application.WalletQueryService;
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

    private final ListingOpenService listingOpenService;
    private final MarketplaceService marketplaceService;
    private final ShopService shopService;
    private final TopUpService topUpService;
    private final WalletQueryService walletQueryService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    @Override
    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<EconomyResponse.Listings>> listOpenListings() {
        EconomyResult.ListingSummaries listingSummaries = marketplaceService.listOpen();
        return ApiResponses.ok(EconomyWebMapper.toListings(listingSummaries));
    }

    @Override
    @GetMapping("/listings/me")
    public ResponseEntity<ApiResponse<EconomyResponse.PlayerListings>> myListings() {
        EconomyResult.PlayerListings listings = marketplaceService.listBySeller(currentPlayerAccessor.currentPlayerIdOrThrow());
        return ApiResponses.ok(EconomyWebMapper.toPlayerListings(listings));
    }

    @Override
    @GetMapping("/listings/reservations")
    public ResponseEntity<ApiResponse<EconomyResponse.PlayerReservations>> myReservations() {
        EconomyResult.PlayerReservations reservations = marketplaceService.listReservations(currentPlayerAccessor.currentPlayerIdOrThrow());
        return ApiResponses.ok(EconomyWebMapper.toPlayerReservations(reservations));
    }

    @Override
    @GetMapping("/trades")
    public ResponseEntity<ApiResponse<EconomyResponse.Trades>> myTrades() {
        EconomyResult.Trades trades = marketplaceService.listTrades(currentPlayerAccessor.currentPlayerIdOrThrow());
        return ApiResponses.ok(EconomyWebMapper.toTrades(trades));
    }

    @Override
    @PostMapping("/listings")
    public ResponseEntity<ApiResponse<EconomyResponse.ListingId>> openListing(
            @Valid @RequestBody EconomyRequest.OpenListing request
    ) {
        EconomyResult.ListingId id = listingOpenService.open(EconomyWebMapper.toOpenListingCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toListingId(id));
    }

    @Override
    @PostMapping("/listings/{listingId}/reserve")
    public ResponseEntity<ApiResponse<EconomyResponse.Reservation>> reserveListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.ReserveListing request
    ) {
        EconomyResult.Reservation reservation = marketplaceService.reserve(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toReserveListingCommand(listingId, request));
        return ApiResponses.ok(EconomyWebMapper.toReservation(reservation));
    }

    @Override
    @PostMapping("/listings/{listingId}/purchase")
    public ResponseEntity<ApiResponse<EconomyResponse.Trade>> purchaseListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.PurchaseListing request
    ) {
        EconomyResult.TradeSummary trade = marketplaceService.purchase(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toPurchaseListingCommand(listingId, request));
        return ApiResponses.ok(EconomyWebMapper.toTrade(trade));
    }

    @Override
    @DeleteMapping("/listings/{listingId}")
    public ResponseEntity<ApiResponse<Void>> cancelListing(@PathVariable Long listingId) {
        marketplaceService.cancel(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toCancelListingCommand(listingId));
        return ApiResponses.noContent();
    }

    @Override
    @GetMapping("/shop/items")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopItems>> listShopItems() {
        EconomyResult.ShopItems items = shopService.listAvailableItems();
        return ApiResponses.ok(EconomyWebMapper.toShopItems(items));
    }

    @Override
    @GetMapping("/shop/purchases")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopPurchases>> getMyShopPurchases() {
        EconomyResult.ShopPurchases purchases = shopService.listPurchases(currentPlayerAccessor.currentPlayerIdOrThrow());
        return ApiResponses.ok(EconomyWebMapper.toShopPurchases(purchases));
    }

    @Override
    @PostMapping("/shop/purchase")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseId>> purchaseShopItem(
            @Valid @RequestBody EconomyRequest.PurchaseShopItem request
    ) {
        EconomyResult.ShopPurchaseId id = shopService.purchase(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toPurchaseShopItemCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toShopPurchaseId(id));
    }

    @Override
    @PostMapping("/shop/reservations/confirm")
    public ResponseEntity<ApiResponse<EconomyResponse.ShopReservation>> confirmShopReservation(
            @Valid @RequestBody EconomyRequest.ConfirmShopReservation request
    ) {
        EconomyResult.ShopReservation reservation = shopService.confirmReservation(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toConfirmShopReservationCommand(request));
        return ApiResponses.ok(EconomyWebMapper.toShopReservation(reservation));
    }

    @Override
    @GetMapping("/wallet")
    public ResponseEntity<ApiResponse<EconomyResponse.WalletBalance>> wallet() {
        EconomyResult.WalletSummary balance = walletQueryService.wallet(currentPlayerAccessor.currentPlayerIdOrThrow());
        return ApiResponses.ok(EconomyWebMapper.toWalletBalance(balance));
    }

    @Override
    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<Void>> topUp(@Valid @RequestBody EconomyRequest.TopUp request) {
        topUpService.topUp(currentPlayerAccessor.currentPlayerIdOrThrow(), EconomyWebMapper.toTopUpCommand(request));
        return ApiResponses.noContent();
    }
}
