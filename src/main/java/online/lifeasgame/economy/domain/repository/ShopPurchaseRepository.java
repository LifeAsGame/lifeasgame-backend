package online.lifeasgame.economy.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import online.lifeasgame.economy.domain.ShopPurchase;

public interface ShopPurchaseRepository {
    ShopPurchase save(ShopPurchase purchase);
    Optional<ShopPurchase> findById(Long id);
    Optional<ShopPurchase> findByReservationToken(String token);
    long countCompletedByShopItemId(Long shopItemId);
    long countCompletedByShopItemIdAndPlayerId(Long shopItemId, Long playerId);
    long countActiveReservationsByShopItemId(Long shopItemId, Instant now);
    List<ShopPurchase> findReservableExpiringBefore(Instant instant);
    List<ShopPurchase> findByPlayerId(Long playerId);
    List<ShopPurchase> findAll();
}
