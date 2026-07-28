package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemCode;
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

    private final JpaItemRepository jpaRepository;

    @Override
    public Optional<Item> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Item> findByCode(ItemCode code) {
        return jpaRepository.findByCode(code);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public Item save(Item item) {
        return jpaRepository.save(item);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public Page<Item> search(String name, ItemCategory itemCategory, ItemType itemType, Rarity rarity, Pageable pageable) {
        var spec = ItemSpecifications.search(name, itemCategory, itemType, rarity);
        return jpaRepository.findAll(spec, pageable);
    }

    @Override
    public void delete(Item item) {
        jpaRepository.delete(item);
    }
}
