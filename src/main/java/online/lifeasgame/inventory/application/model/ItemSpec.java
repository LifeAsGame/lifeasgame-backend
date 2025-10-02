package online.lifeasgame.inventory.application.model;

import online.lifeasgame.inventory.application.command.ItemCommand;
import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemName;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;

public final class ItemSpec {

    private ItemSpec() {}

    public record Create(
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {
        public static Create from(ItemCommand.Create cmd) {
            return new Create(
                    ItemName.of(cmd.name()),
                    ItemCategory.parse(cmd.category()),
                    ItemType.parse(cmd.type()),
                    Rarity.parse(cmd.rarity()),
                    BaseAttrs.of(cmd.baseAttrs()),
                    cmd.stackable(),
                    cmd.maxStack(),
                    cmd.maxDurability()
            );
        }
    }

    public record Update(
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurability
    ) {
        public static Update from(ItemCommand.Update cmd) {
            return new Update(
                    ItemName.ofNullable(cmd.name()),
                    ItemCategory.parseNullable(cmd.category()),
                    ItemType.parseNullable(cmd.type()),
                    Rarity.parseNullable(cmd.rarity()),
                    BaseAttrs.of(cmd.baseAttrs()),
                    cmd.stackable(),
                    cmd.maxStack(),
                    cmd.maxDurability()
            );
        }
    }
}
