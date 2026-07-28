package online.lifeasgame.inventory.domain.seed;

import online.lifeasgame.inventory.domain.BaseAttrs;
import online.lifeasgame.inventory.domain.ItemCategory;
import online.lifeasgame.inventory.domain.ItemType;
import online.lifeasgame.inventory.domain.Rarity;

import java.util.Objects;

public record ItemSeedDefinition(
        ItemContentCode code,
        String name,
        ItemCategory category,
        ItemType type,
        Rarity rarity,
        BaseAttrs baseAttrs,
        boolean stackable,
        int maxStack,
        Integer maxDurability
) {

    public ItemSeedDefinition {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(baseAttrs, "baseAttrs");
    }
}
