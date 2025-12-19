package online.lifeasgame.economy.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.error.EconomyError;
import online.lifeasgame.economy.domain.repository.ShopItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ShopItemReader {

    private final ShopItemRepository repository;

    public ShopItem getForUpdate(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new DomainException(EconomyError.SHOP_ITEM_NOT_FOUND));
    }

    public List<ShopItem> listAvailable() {
        return repository.findAvailable();
    }

    public List<ShopItem> listAll() {
        return repository.findAll();
    }
}
