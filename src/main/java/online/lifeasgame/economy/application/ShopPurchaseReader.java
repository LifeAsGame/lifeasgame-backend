package online.lifeasgame.economy.application;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ShopPurchaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ShopPurchaseReader {

    private final ShopPurchaseRepository shopPurchaseRepository;

    public ShopPurchase getByReservationToken(String reservationToken) {
        return shopPurchaseRepository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public long countCompleted(Long shopItemId) {
        return shopPurchaseRepository.countCompletedByShopItemId(shopItemId);
    }

    public long countReserved(Long shopItemId, Instant now) {
        return shopPurchaseRepository.countActiveReservationsByShopItemId(shopItemId, now);
    }

    public long countCompletedByPlayer(Long shopItemId, Long playerId) {
        return shopPurchaseRepository.countCompletedByShopItemIdAndPlayerId(shopItemId, playerId);
    }

    public List<ShopPurchase> findExpiringBefore(Instant cutoff) {
        return shopPurchaseRepository.findReservableExpiringBefore(cutoff);
    }

    public List<ShopPurchase> findByPlayer(Long playerId) {
        return shopPurchaseRepository.findByPlayerId(playerId);
    }

    public List<ShopPurchase> findAll() {
        return shopPurchaseRepository.findAll();
    }
}
