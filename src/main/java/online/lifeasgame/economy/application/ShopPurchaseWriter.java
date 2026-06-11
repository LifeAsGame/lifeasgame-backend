package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ShopPurchase;
import online.lifeasgame.economy.domain.repository.ShopPurchaseRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ShopPurchaseWriter {

    private final ShopPurchaseRepository repository;

    public ShopPurchase create(ShopPurchase shopPurchase) {
        return repository.save(shopPurchase);
    }
}
