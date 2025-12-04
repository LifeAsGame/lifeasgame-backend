package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.Money;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.repository.ShopItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ShopItemWriter {

    private final ShopItemRepository shopItemRepository;

    public ShopItem create(Long itemId, Money price, Integer globalLimit, Integer perPlayerLimit, Integer reservationTtlSec) {
        ShopItem item = ShopItem.createLimited(itemId, price, globalLimit, perPlayerLimit, reservationTtlSec);
        return shopItemRepository.save(item);
    }

    public ShopItem save(ShopItem item) {
        return shopItemRepository.save(item);
    }
}
