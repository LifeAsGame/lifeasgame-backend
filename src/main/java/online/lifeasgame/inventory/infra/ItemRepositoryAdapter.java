package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import online.lifeasgame.inventory.domain.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ItemRepositoryAdapter implements ItemRepository {

    private final JpaItemRepository jpa;

    @Override
    public Optional<Item> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public Item save(Item item) {
        return jpa.save(item);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public Page<Item> search(String q, ItemCategory category, ItemType type, Rarity rarity, Pageable pageable) {
        var spec = ItemSpecifications.search(q, category, type, rarity);
        return jpa.findAll(spec, pageable);
    }

    @Override
    public void delete(Item item) {
        jpa.delete(item);
    }
}
