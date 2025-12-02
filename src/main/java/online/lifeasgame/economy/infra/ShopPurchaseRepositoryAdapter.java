package online.lifeasgame.economy.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.repository.ShopPurchaseRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShopPurchaseRepositoryAdapter implements ShopPurchaseRepository {

    private final JpaShopPurchaseRepository jpaShopPurchaseRepository;

    @Override
    public ShopPurchase save(ShopPurchase purchase) {
        return jpaShopPurchaseRepository.save(purchase);
    }

    @Override
    public Optional<ShopPurchase> findById(Long id) {
        return jpaShopPurchaseRepository.findById(id);
    }

    @Override
    public Optional<ShopPurchase> findByReservationToken(String token) {
        return jpaShopPurchaseRepository.findByReservationToken(token);
    }

    @Override
    public long countCompletedByShopItemId(Long shopItemId) {
        return jpaShopPurchaseRepository.countByShopItemIdAndStatus(shopItemId, ShopPurchase.Status.COMPLETED);
    }

    @Override
    public long countCompletedByShopItemIdAndPlayerId(Long shopItemId, Long playerId) {
        return jpaShopPurchaseRepository.countByShopItemIdAndPlayerIdAndStatus(shopItemId, playerId, ShopPurchase.Status.COMPLETED);
    }

    @Override
    public long countActiveReservationsByShopItemId(Long shopItemId, Instant now) {
        return jpaShopPurchaseRepository.countActiveReservations(shopItemId, now);
    }

    @Override
    public List<ShopPurchase> findReservableExpiringBefore(Instant instant) {
        return jpaShopPurchaseRepository.findByStatusAndReservationExpiresAtBefore(ShopPurchase.Status.RESERVED, instant);
    }

    @Override
    public List<ShopPurchase> findByPlayerId(Long playerId) {
        return jpaShopPurchaseRepository.findByPlayerId(playerId);
    }

    @Override
    public List<ShopPurchase> findAll() {
        return jpaShopPurchaseRepository.findAll();
    }
}
