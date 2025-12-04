package online.lifeasgame.economy.domain.repository;

import java.util.Optional;
import online.lifeasgame.economy.domain.ShopItem;
import java.util.List;

public interface ShopItemRepository {
    ShopItem save(ShopItem shopItem);
    Optional<ShopItem> findById(Long id);
    Optional<ShopItem> findByIdForUpdate(Long id);
    List<ShopItem> findAvailable();
    List<ShopItem> findAll();
}
