package online.lifeasgame.economy.api.admin.spec;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.admin.request.AdminEconomyRequest;
import online.lifeasgame.economy.api.admin.response.AdminEconomyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AdminEconomyApiSpecV1 {

    @Operation(
            summary = "상점 아이템 목록 조회",
            description = "관리자용 상점 아이템 전체 목록을 조회합니다. 비활성 아이템 포함 여부는 정책에 따릅니다."
    )
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItems>> listShopItems();

    @Operation(
            summary = "상점 아이템 생성",
            description = "신규 상점 아이템을 생성합니다. 가격, 재화 타입, 판매 가능 수량 등은 도메인 정책에 따라 검증됩니다."
    )
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> createShopItem(
            @Valid @RequestBody AdminEconomyRequest.CreateShopItem request
    );

    @Operation(
            summary = "상점 아이템 수정",
            description = "기존 상점 아이템 정보를 수정합니다. 활성화 상태 또는 판매 이력에 따라 일부 필드는 제한될 수 있습니다."
    )
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> updateShopItem(
            @PathVariable Long shopItemId,
            @Valid @RequestBody AdminEconomyRequest.UpdateShopItem request
    );

    @Operation(
            summary = "상점 아이템 활성/비활성 전환",
            description = "상점 아이템의 판매 가능 상태를 토글합니다. 비활성화 시 신규 구매는 불가합니다."
    )
    ResponseEntity<ApiResponse<Void>> toggleShopItem(
            @PathVariable Long shopItemId,
            @Valid @RequestBody AdminEconomyRequest.ToggleShopItem request
    );

    @Operation(
            summary = "상점 구매 내역 조회",
            description = "전체 플레이어의 상점 구매/예약 내역을 조회합니다. 감사 및 운영 관리 목적입니다."
    )
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopPurchases>> listShopPurchases();

    @Operation(
            summary = "플레이어 지갑 수동 조정",
            description = "특정 플레이어의 지갑 잔액을 관리자 권한으로 조정합니다. 보상, 환불, 제재 등에 사용됩니다."
    )
    ResponseEntity<ApiResponse<AdminEconomyResponse.WalletBalance>> adjustWallet(
            @PathVariable @Positive Long playerId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminEconomyRequest.AdjustWallet request
    );
}
