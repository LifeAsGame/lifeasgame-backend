package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;
import online.lifeasgame.inventory.domain.error.ItemError;
import online.lifeasgame.inventory.domain.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ItemReader {

    private final ItemRepository itemRepository;

    public Item getItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new DomainException(ItemError.ITEM_NOT_FOUND));
    }

    public Page<Item> search(
            String name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            Pageable pageable
    ) {
        return itemRepository.search(name, category, type, rarity, pageable);
    }

    public boolean existsByName(String name) {
        if(name==null) return false;
        return itemRepository.existsByName(name.trim());
    }

    public boolean existsById(Long id) {
        return itemRepository.existsById(id);
    }
}
