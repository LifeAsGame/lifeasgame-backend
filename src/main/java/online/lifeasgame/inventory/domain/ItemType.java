package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.inventory.domain.error.ItemError;

import java.util.List;

public enum ItemType {
    SWORD,
    BOW,
    STAFF,
    SHIELD,
    HELMET,
    CHEST,
    RING,
    POTION,
    SCROLL,
    ORE,
    HERB,
    KEY,
    ETC
    ;

    public static ItemType parse(String raw) {
        return EnumParsers.parseStrict(
                ItemType.class,
                raw,
                ItemError.INVALID_ITEM_TYPE,
                "Item type"
        );
    }

    public static List<ItemType> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                ItemType.class,
                raw,
                ItemError.INVALID_ITEM_TYPE,
                "Item types"
        );
    }
}
