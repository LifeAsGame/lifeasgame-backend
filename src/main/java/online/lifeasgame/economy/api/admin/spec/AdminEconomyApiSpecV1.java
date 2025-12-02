package online.lifeasgame.economy.api.admin.spec;

import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.economy.api.admin.request.AdminEconomyRequest;
import online.lifeasgame.economy.api.admin.response.AdminEconomyResponse;
import org.springframework.http.ResponseEntity;

public interface AdminEconomyApiSpecV1 {
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> createShopItem(AdminEconomyRequest.CreateShopItem request);
    ResponseEntity<ApiResponse<Void>> toggleShopItem(Long shopItemId, AdminEconomyRequest.ToggleShopItem request);
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItem>> updateShopItem(Long shopItemId, AdminEconomyRequest.UpdateShopItem request);
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopItems>> listShopItems();
    ResponseEntity<ApiResponse<AdminEconomyResponse.ShopPurchases>> listShopPurchases();
    ResponseEntity<ApiResponse<AdminEconomyResponse.WalletBalance>> adjustWallet(Long playerId, AdminEconomyRequest.AdjustWallet request);
}
