package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

public enum QuestProgressSource {
    RECORD_CREATED,
    COUNT,
    MANUAL_CHECK;

    public static QuestProgressSource parse(String raw) {
        return EnumParsers.parseStrict(
                QuestProgressSource.class,
                raw,
                QuestError.INVALID_QUEST_PROGRESS_SOURCE,
                "Quest progress source"
        );
    }

    public static QuestProgressSource parseNullable(String raw) {
        return raw == null ? null : parse(raw);
    }
}
