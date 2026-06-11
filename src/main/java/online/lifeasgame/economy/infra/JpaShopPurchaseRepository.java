package online.lifeasgame.economy.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import online.lifeasgame.economy.domain.ShopPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaShopPurchaseRepository extends JpaRepository<ShopPurchase, Long> {
    Optional<ShopPurchase> findByReservationToken(String token);

    long countByShopItemIdAndStatus(Long shopItemId, ShopPurchase.Status status);

    long countByShopItemIdAndPlayerIdAndStatus(Long shopItemId, Long playerId, ShopPurchase.Status status);

    @Query("SELECT COUNT(sp) FROM ShopPurchase sp WHERE sp.shopItemId = :shopItemId AND sp.status = 'RESERVED' AND sp.reservationExpiresAt > :now")
    long countActiveReservations(Long shopItemId, Instant now);

    List<ShopPurchase> findByStatusAndReservationExpiresAtBefore(ShopPurchase.Status status, Instant instant);

    List<ShopPurchase> findByPlayerId(Long playerId);
}
