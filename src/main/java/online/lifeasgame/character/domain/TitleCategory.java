package online.lifeasgame.character.domain;

import java.util.List;
import online.lifeasgame.character.domain.error.TitleError;
import online.lifeasgame.core.lang.EnumParsers;

public enum TitleCategory {
    ACHIEVEMENT,
    EVENT,
    QUEST,
    RANKED,
    SPECIAL,
    OTHER
    ;

    public static TitleCategory parse(String raw) {
        return EnumParsers.parseStrict(
                TitleCategory.class,
                raw,
                TitleError.INVALID_TITLE_CATEGORY,
                "Title category"
        );
    }

    public static List<TitleCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                TitleCategory.class,
                raw,
                TitleError.INVALID_TITLE_CATEGORY,
                "Title categories"
        );
    }
}
