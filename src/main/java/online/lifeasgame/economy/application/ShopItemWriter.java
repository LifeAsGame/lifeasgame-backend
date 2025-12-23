package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.repository.ShopItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ShopItemWriter {

    private final ShopItemRepository repository;

    public ShopItem create(ShopItem shopItem) {
        return repository.save(shopItem);
    }
}
