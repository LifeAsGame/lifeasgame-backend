package online.lifeasgame.inventory.domain.repository;

import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ItemRepository {
    Optional<Item> findById(Long id);

    boolean existsById(Long id);

    Item save(Item item);

    boolean existsByName(String name);

    Page<Item> search(String q, ItemCategory category, ItemType type, Rarity rarity, Pageable pageable);

    void delete(Item item);
}
