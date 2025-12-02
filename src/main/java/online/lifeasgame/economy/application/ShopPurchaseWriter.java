package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.repository.ShopPurchaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ShopPurchaseWriter {

    private final ShopPurchaseRepository shopPurchaseRepository;

    public ShopPurchase request(Long shopItemId, Long playerId, int quantity, Money price) {
        return shopPurchaseRepository.save(ShopPurchase.request(shopItemId, playerId, quantity, price));
    }

    public ShopPurchase save(ShopPurchase purchase) {
        return shopPurchaseRepository.save(purchase);
    }
}
