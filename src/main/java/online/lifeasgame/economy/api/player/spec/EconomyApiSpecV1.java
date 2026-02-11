package online.lifeasgame.economy.api.player.spec;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Economy API V1 (Player)")
public interface EconomyApiSpecV1 {

    @Operation(summary = "거래소 오픈 리스팅 조회", description = "OPEN 상태의 리스팅을 조회합니다. (텍스트 UI를 위해 페이징/필터 권장)")
    ResponseEntity<ApiResponse<EconomyResponse.Listings>> listOpenListings(
//            @RequestParam(required = false) Long itemId,
//            @RequestParam(required = false) Long sellerId,
//            @RequestParam(defaultValue = "NEWEST") String sort,
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "내 리스팅 목록", description = "내가 올린 리스팅 목록을 조회합니다.")
    ResponseEntity<ApiResponse<EconomyResponse.PlayerListings>> myListings(
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "내 예약 목록", description = "내가 예약한 리스팅 목록을 조회합니다.")
    ResponseEntity<ApiResponse<EconomyResponse.PlayerReservations>> myReservations(
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "내 거래 내역", description = "내가 참여한 거래(구매/판매) 내역을 조회합니다.")
    ResponseEntity<ApiResponse<EconomyResponse.Trades>> myTrades(
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "리스팅 등록(판매글 오픈)")
    ResponseEntity<ApiResponse<EconomyResponse.ListingId>> openListing(
            @Valid @RequestBody EconomyRequest.OpenListing request
    );

    @Operation(summary = "리스팅 예약(구매자)", description = "예약 시 지갑에서 hold가 잡히고 reservationToken이 발급됩니다.")
    ResponseEntity<ApiResponse<EconomyResponse.Reservation>> reserveListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.ReserveListing request
    );

    @Operation(summary = "리스팅 구매 확정")
    ResponseEntity<ApiResponse<EconomyResponse.Trade>> purchaseListing(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.PurchaseListing request
    );

    @Operation(summary = "리스팅 취소(판매자)")
    ResponseEntity<ApiResponse<Void>> cancelListing(@PathVariable Long listingId);

    @Operation(summary = "상점 아이템 목록 조회")
    ResponseEntity<ApiResponse<EconomyResponse.ShopItems>> listShopItems();

    @Operation(summary = "내 상점 구매 목록들 조회")
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchases>> getMyShopPurchases();

    @Operation(summary = "상점 아이템 구매(즉시 구매 또는 예약)")
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseId>> purchaseShopItem(
            @Valid @RequestBody EconomyRequest.PurchaseShopItem request
    );

    @Operation(summary = "상점 예약 확정(결제/hold commit)")
    ResponseEntity<ApiResponse<EconomyResponse.ShopReservation>> confirmShopReservation(
            @Valid @RequestBody EconomyRequest.ConfirmShopReservation request
    );

    @Operation(summary = "지갑 잔액 조회")
    ResponseEntity<ApiResponse<EconomyResponse.WalletBalance>> wallet();

    @Operation(summary = "충전(Top-up)")
    ResponseEntity<ApiResponse<Void>> topUp(@Valid @RequestBody EconomyRequest.TopUp request);
}
