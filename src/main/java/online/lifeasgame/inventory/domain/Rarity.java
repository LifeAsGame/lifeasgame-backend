package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.inventory.domain.error.ItemError;

import java.util.List;

public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY
    ;

    public static Rarity parse(String raw) {
        if (raw == null || raw.isEmpty()) return COMMON;

        return EnumParsers.parseStrict(
                Rarity.class,
                raw,
                ItemError.INVALID_ITEM_RARITY,
                "Item rarity"
        );
    }

    public static Rarity parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return EnumParsers.parseStrict(
                Rarity.class,
                raw,
                ItemError.INVALID_ITEM_RARITY,
                "Item rarity"
        );
    }

    public static List<Rarity> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                Rarity.class,
                raw,
                ItemError.INVALID_ITEM_RARITY,
                "Item rarities"
        );
    }
}
