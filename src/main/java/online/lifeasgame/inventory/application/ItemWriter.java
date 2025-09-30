package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.model.ItemSpec;
import online.lifeasgame.inventory.domain.Item;
import online.lifeasgame.inventory.domain.error.ItemError;
import online.lifeasgame.inventory.domain.repository.ItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ItemWriter {

    private final ItemRepository itemRepository;

    public Item create(ItemSpec.Create itemSpec) {
        return itemRepository.save(
                Item.create(
                        itemSpec.name(),
                        itemSpec.category(),
                        itemSpec.type(),
                        itemSpec.rarity(),
                        itemSpec.baseAttrs(),
                        itemSpec.stackable(),
                        itemSpec.maxStack(),
                        itemSpec.maxDurability()
                )
        );
    }

    public Item update(Item item, ItemSpec.Update itemSpec) {
        item.update(
                itemSpec.name(),
                itemSpec.category(),
                itemSpec.type(),
                itemSpec.rarity(),
                itemSpec.baseAttrs(),
                itemSpec.stackable(),
                itemSpec.maxStack(),
                itemSpec.maxDurability()
        );

        return item;
    }

    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new DomainException(ItemError.ITEM_NOT_FOUND);
        }
        itemRepository.deleteById(id);
    }
}
