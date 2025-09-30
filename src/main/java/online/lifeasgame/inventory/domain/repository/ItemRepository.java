package online.lifeasgame.inventory.domain.repository;

import java.util.Optional;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepository {
    Optional<Item> findById(Long id);

    boolean existsById(Long id);

    Item save(Item item);

    void deleteById(Long id);

    boolean existsByName(String name);

    Page<Item> search(String q, ItemCategory category, ItemType type, Rarity rarity, Pageable pageable);
}
