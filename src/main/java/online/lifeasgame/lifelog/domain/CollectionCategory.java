package online.lifeasgame.lifelog.domain;


import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.lifelog.domain.error.LifeLogError;

public enum CollectionCategory {
    FIGURE, CARD, BOOK, GAME, STAMP, COIN, OTHER;

    public static CollectionCategory parse(String raw) {
        return EnumParsers.parseStrict(
                CollectionCategory.class,
                raw,
                LifeLogError.INVALID_COLLECTION_CATEGORY,
                "Collection Category"
        );
    }

    public static CollectionCategory parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
