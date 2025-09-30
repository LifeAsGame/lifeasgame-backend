package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.model.ItemSpec;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.*;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemReader itemReader;
    private final ItemWriter itemWriter;

    @Transactional
    public ItemResult.Id create(ItemCommand.Create cmd) {
        if (itemReader.existsByName(cmd.name())) {
            throw new DomainException(ItemError.ITEM_NAME_DUP);
        }

        Item saved = itemWriter.create(ItemSpec.Create.from(cmd));
        return ItemResult.Id.of(saved.getId());
    }

    @Transactional
    public ItemResult.Id update(ItemCommand.Update cmd) {
        Item item = itemReader.getItem(cmd.id());
        itemWriter.update(item, ItemSpec.Update.from(cmd));
        return ItemResult.Id.of(cmd.id());
    }

    @Transactional
    public ItemResult.Deleted delete(Long id) {
        if (!itemReader.existsById(id)) {
            throw new DomainException(ItemError.ITEM_NOT_FOUND);
        }

        itemWriter.delete(id);

        return ItemResult.Deleted.of(id);
    }

    @Transactional(readOnly = true)
    public ItemResult.Detail getItem(Long id) {
        Item item = itemReader.getItem(id);

        Integer maxDurability = item.durabilityPolicy()
                .map(DurabilityPolicy::max)
                .orElse(null);

        Map<String, Integer> attrs = item.getBaseAttrs().attrs();
        return ItemResult.Detail.of(
                item.getId(),
                item.getName().value(),
                item.getCategory().name(),
                item.getType().name(),
                item.getRarity().name(),
                item.isStackable(),
                item.maxStack(),
                maxDurability,
                attrs
        );
    }

    @Transactional(readOnly = true)
    public ItemResult.Page<ItemResult.Summary> search(
            String name,
            String category,
            String type,
            String rarity,
            Pageable pageable
    ) {
        Page<ItemResult.Summary> page = itemReader.search(
                name,
                ItemCategory.parse(category),
                ItemType.parse(type),
                Rarity.parse(rarity),
                pageable
        ).map(ItemResult.Summary::from);

        return ItemResult.Page.of(page);
    }
}
