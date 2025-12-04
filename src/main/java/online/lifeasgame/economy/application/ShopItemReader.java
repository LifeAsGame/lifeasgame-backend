package online.lifeasgame.economy.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ShopItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ShopItemReader {

    private final ShopItemRepository shopItemRepository;

    public ShopItem getForUpdate(Long id) {
        return shopItemRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException(EconomyError.SHOP_ITEM_NOT_FOUND));
    }

    public List<ShopItem> listAvailable() {
        return shopItemRepository.findAvailable();
    }

    public List<ShopItem> listAll() {
        return shopItemRepository.findAll();
    }
}
