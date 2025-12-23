package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ShopPurchaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ShopPurchaseReader {

    private final ShopPurchaseRepository repository;

    public ShopPurchase getByReservationToken(String reservationToken) {
        return repository.findByReservationToken(reservationToken)
                .orElseThrow(() -> new DomainException(EconomyError.LISTING_NOT_FOUND));
    }

    public long countCompleted(Long shopItemId) {
        return repository.countCompletedByShopItemId(shopItemId);
    }

    public long countReserved(Long shopItemId, Instant now) {
        return repository.countActiveReservationsByShopItemId(shopItemId, now);
    }

    public long countCompletedByPlayer(Long shopItemId, Long playerId) {
        return repository.countCompletedByShopItemIdAndPlayerId(shopItemId, playerId);
    }

    public List<ShopPurchase> findExpiringBefore(Instant cutoff) {
        return repository.findReservableExpiringBefore(cutoff);
    }

    public List<ShopPurchase> findByPlayer(Long playerId) {
        return repository.findByPlayerId(playerId);
    }

    public List<ShopPurchase> findAll() {
        return repository.findAll();
    }
}
