package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.inventory.domain.error.ItemError;

import java.util.List;

public enum ItemCategory {
    WEAPON,
    ARMOR,
    ACCESSORY,
    CONSUMABLE,
    MATERIAL,
    QUEST,
    MISC
    ;

    public static ItemCategory parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        return EnumParsers.parseStrict(
                ItemCategory.class,
                raw,
                ItemError.INVALID_ITEM_CATEGORY,
                "Item category"
        );
    }

    public static List<ItemCategory> parse(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        return EnumParsers.parseListStrict(
                ItemCategory.class,
                raw,
                ItemError.INVALID_ITEM_CATEGORY,
                "Item categories"
        );
    }
}
