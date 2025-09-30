package online.lifeasgame.inventory.domain;

import java.util.List;
import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.inventory.domain.error.ItemError;

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

    public static List<Rarity> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                Rarity.class,
                raw,
                ItemError.INVALID_ITEM_RARITY,
                "Item rarities"
        );
    }
}
