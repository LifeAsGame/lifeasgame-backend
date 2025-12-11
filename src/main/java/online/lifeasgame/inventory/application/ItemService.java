package online.lifeasgame.inventory.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.application.query.ItemStackPolicyQuery;
import online.lifeasgame.inventory.application.result.ItemResult;
import online.lifeasgame.inventory.domain.*;
import online.lifeasgame.inventory.domain.error.ItemError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemReader itemReader;
    private final ItemWriter itemWriter;
    private final ItemStackPolicyQuery itemStackPolicyQuery;

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
    public ItemResult.Id update(ItemCommand.Update cmd) {
        Item item = itemReader.getByIdOrThrow(cmd.id());

        if (!item.getName().value().equals(cmd.name())) {
            itemReader.assertNameNotExists(cmd.name());
        }

        int newLimit = !cmd.stackable() ? 1 : (cmd.maxStack() == null ? 1 : cmd.maxStack());
        long violating = itemStackPolicyQuery.countTotalStacksExceeding(item.getId(), newLimit);
        if (violating > 0) {
            throw new DomainException(ItemError.POLICY_CONFLICT);
        }

        item.update(
                ItemName.of(cmd.name()),
                ItemCategory.parse(cmd.category()),
                ItemType.parse(cmd.type()),
                Rarity.parse(cmd.rarity()),
                BaseAttrs.of(cmd.baseAttrs()),
                cmd.stackable(),
                cmd.maxStack(),
                cmd.maxDurability()
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
