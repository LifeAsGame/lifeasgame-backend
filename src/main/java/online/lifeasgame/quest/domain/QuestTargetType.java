package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.List;

public enum QuestTargetType {
    MINUTES, COUNT, SCORE;

    public static QuestTargetType parse(String raw) {
        return EnumParsers.parseStrict(
                QuestTargetType.class,
                raw,
                QuestError.INVALID_QUEST_TARGET_TYPE,
                "Quest target type"
        );
    }

    public static QuestTargetType parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<QuestTargetType> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestTargetType.class,
                raw,
                QuestError.INVALID_QUEST_TARGET_TYPE,
                "Quest target types"
        );
    }
}
