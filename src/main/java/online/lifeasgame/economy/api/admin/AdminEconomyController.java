package online.lifeasgame.economy.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.admin.mapper.AdminEconomyWebMapper;
import online.lifeasgame.economy.api.admin.request.AdminEconomyRequest;
import online.lifeasgame.economy.api.admin.response.AdminEconomyResponse;
import online.lifeasgame.economy.api.admin.spec.AdminEconomyApiSpecV1;
import online.lifeasgame.economy.application.AdminWalletAdjustmentService;
import online.lifeasgame.economy.application.result.EconomyResult;
import online.lifeasgame.economy.application.ShopService;
import online.lifeasgame.platform.web.response.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/economy")
public class AdminEconomyController implements AdminEconomyApiSpecV1 {

    private final ShopService shopService;
    private final AdminWalletAdjustmentService walletAdjustmentService;

    @Override
    @GetMapping("/shop/items")
    public ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItems>> listShopItems() {
        EconomyResult.ShopItems result = shopService.listAllItems();
        return ApiResponses.ok(AdminEconomyWebMapper.toShopItems(result));
    }

    @Override
    @PostMapping("/shop/items")
    public ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> createShopItem(
            @Valid @RequestBody AdminEconomyRequest.CreateShopItem request
    ) {
        EconomyResult.ShopItemView result = shopService.createItem(
                AdminEconomyWebMapper.toCreateShopItemCommand(request)
        );

        return ApiResponses.ok(AdminEconomyWebMapper.toShopItem(result));
    }

    @Override
    @PatchMapping("/shop/items/{shopItemId}")
    public ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> updateShopItem(
            @PathVariable Long shopItemId,
            @Valid @RequestBody AdminEconomyRequest.UpdateShopItem request
    ) {
        EconomyResult.ShopItemView result = shopService.updateLimits(
                AdminEconomyWebMapper.toUpdateShopItemCommand(shopItemId, request)
        );

        return ApiResponses.ok(AdminEconomyWebMapper.toShopItem(result));
    }

    @Override
    @PatchMapping("/shop/items/{shopItemId}/availability")
    public ResponseEntity<ApiResponse<Void>> toggleShopItem(
            @PathVariable Long shopItemId,
            @Valid @RequestBody AdminEconomyRequest.ToggleShopItem request
    ) {
        shopService.toggleAvailability(AdminEconomyWebMapper.toToggleShopItemCommand(shopItemId, request));
        return ApiResponses.noContent();
    }

    @Override
    @GetMapping("/shop/purchases")
    public ResponseEntity<ApiResponse<AdminEconomyResponse.ShopPurchases>> listShopPurchases() {
        EconomyResult.ShopPurchases result = shopService.listAllPurchases();
        return ApiResponses.ok(AdminEconomyWebMapper.toShopPurchases(result));
    }

    @Override
    @PostMapping("/wallets/{playerId}/adjust")
    public ResponseEntity<ApiResponse<AdminEconomyResponse.WalletBalance>> adjustWallet(
            @PathVariable @Positive Long playerId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,99}")
            String correlationId,
            @Valid @RequestBody AdminEconomyRequest.AdjustWallet request
    ) {
        EconomyResult.WalletBalance result = walletAdjustmentService.adjust(
                AdminEconomyWebMapper.toAdjustWalletCommand(
                        playerId,
                        request,
                        idempotencyKey,
                        correlationId
                )
        );

        return ApiResponses.ok(AdminEconomyWebMapper.toWalletBalance(result));
    }
}
