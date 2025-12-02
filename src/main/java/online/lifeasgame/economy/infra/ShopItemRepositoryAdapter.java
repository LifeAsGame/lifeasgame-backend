package online.lifeasgame.economy.infra;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.ShopItem;
import online.lifeasgame.economy.domain.repository.ShopItemRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShopItemRepositoryAdapter implements ShopItemRepository {

    private final JpaShopItemRepository jpaShopItemRepository;

    @Override
    public ShopItem save(ShopItem shopItem) {
        return jpaShopItemRepository.save(shopItem);
    }

    @Override
    public Optional<ShopItem> findById(Long id) {
        return jpaShopItemRepository.findById(id);
    }

    @Override
    public Optional<ShopItem> findByIdForUpdate(Long id) {
        return jpaShopItemRepository.findByIdForUpdate(id);
    }

    @Override
    public List<ShopItem> findAvailable() {
        return jpaShopItemRepository.findByAvailableTrue();
    }

    @Override
    public List<ShopItem> findAll() {
        return jpaShopItemRepository.findAll();
    }
}
