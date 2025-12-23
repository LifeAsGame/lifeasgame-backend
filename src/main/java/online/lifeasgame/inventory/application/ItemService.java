package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemReader itemReader;
    private final ItemWriter itemWriter;
    private final ItemStackPolicyChecker itemStackPolicyChecker;

    @Transactional
    public ItemResult.Id create(ItemCommand.Create command) {
        Item saved = itemWriter.create(
                Item.create(
                        ItemName.of(command.name()),
                        ItemCategory.parse(command.category()),
                        ItemType.parse(command.type()),
                        Rarity.parse(command.rarity()),
                        BaseAttrs.of(command.baseAttrs()),
                        command.stackable(),
                        command.maxStack(),
                        command.maxDurability()
                )
        );

        return new ItemResult.Id(saved.getId());
    }

    @Transactional
    public ItemResult.Id update(ItemCommand.Update command) {
        Item item = itemReader.getByIdOrThrow(command.id());

        if (!item.getName().value().equals(command.name())) {
            itemReader.assertNameNotExists(command.name());
        }

        int newLimit = !command.stackable() ? 1 : (command.maxStack() == null ? 1 : command.maxStack());
        itemStackPolicyChecker.assertNoPolicyConflict(item.getId(), newLimit);

        item.update(
                ItemName.of(command.name()),
                ItemCategory.parse(command.category()),
                ItemType.parse(command.type()),
                Rarity.parse(command.rarity()),
                BaseAttrs.of(command.baseAttrs()),
                command.stackable(),
                command.maxStack(),
                command.maxDurability()
        );

        return new ItemResult.Id(item.getId());
    }

    @Transactional
    public ItemResult.Deleted delete(Long id) {
        Item item = itemReader.getByIdOrThrow(id);
        itemWriter.delete(item);
        return new ItemResult.Deleted(item.getId());
    }

    @Transactional(readOnly = true)
    public ItemResult.Detail getItem(Long id) {
        Item item = itemReader.getByIdOrThrow(id);
        return ItemResult.Detail.from(item);
    }

    @Transactional(readOnly = true)
    public ItemResult.Page<ItemResult.Summary> search(
            String name,
            String category,
            String type,
            String rarity,
            Pageable pageable
    ) {
        Page<ItemResult.Summary> result = itemReader.search(
                name,
                ItemCategory.parseNullable(category),
                ItemType.parseNullable(type),
                Rarity.parseNullable(rarity),
                pageable
        ).map(ItemResult.Summary::from);

        return ItemResult.Page.from(result);
    }
}
