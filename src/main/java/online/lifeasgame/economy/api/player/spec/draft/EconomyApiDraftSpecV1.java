package online.lifeasgame.economy.api.player.spec.draft;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.player.request.EconomyRequest;
import online.lifeasgame.economy.api.player.response.EconomyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface EconomyApiDraftSpecV1 {


    @Operation(summary = "리스팅 가격 변경", description = "OPEN 상태에서만 변경 가능(텍스트 UI에서 가격 조정 액션용)")
    ResponseEntity<ApiResponse<EconomyResponse.ListingDetail>> changeListingPrice(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.ChangeListingPrice request
    );

    @Operation(summary = "리스팅 예약 취소(구매자)", description = "예약 토큰으로 예약을 해제하고 hold를 취소합니다. (추가 권장)")
    ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable Long listingId,
            @Valid @RequestBody EconomyRequest.CancelReservation request
    );

    @Operation(summary = "상점 아이템 상세 조회", description = "텍스트 UI에서 상세 보기용(추가 권장)")
    ResponseEntity<ApiResponse<EconomyResponse.ShopItem>> getShopItem(
            @PathVariable Long shopItemId
    );

    @Operation(summary = "상점 구매/예약 상세", description = "예약 토큰/만료시간 확인용(추가 권장)")
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseSummary>> getMyShopPurchase(
            @PathVariable Long shopPurchaseId
    );

    @Operation(summary = "내 상점 구매/예약 목록")
    ResponseEntity<ApiResponse<EconomyResponse.ShopPurchaseSummary>> myShopPurchase(
//            @RequestParam(defaultValue = "0") @Min(0) int page,
//            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    );

    @Operation(summary = "상점 예약 취소", description = "예약을 취소하고 hold/redis reservation을 해제합니다. (추가 권장)")
    ResponseEntity<ApiResponse<Void>> cancelShopReservation(
            @Valid @RequestBody EconomyRequest.CancelShopReservation request
    );
}
