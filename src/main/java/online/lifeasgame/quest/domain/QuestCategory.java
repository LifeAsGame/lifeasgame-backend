package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.List;

public enum QuestCategory {
    MAIN, RECOMMENDED, REPEAT, PARTY, GUILD;

    public static QuestCategory parse(String raw) {
        return EnumParsers.parseStrict(
                QuestCategory.class,
                raw,
                QuestError.INVALID_QUEST_CATEGORY,
                "Quest category"
        );
    }

    public static QuestCategory parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<QuestCategory> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestCategory.class,
                raw,
                QuestError.INVALID_QUEST_CATEGORY,
                "Quest categories"
        );
    }
}
