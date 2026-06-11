package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.List;

public enum QuestStatus {
    IN_PROGRESS, DONE, CANCELED;

    public static QuestStatus parse(String raw) {
        return EnumParsers.parseStrict(
                QuestStatus.class,
                raw,
                QuestError.INVALID_QUEST_STATUS,
                "Quest Status"
        );
    }

    public static List<QuestStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestStatus.class,
                raw,
                QuestError.INVALID_QUEST_STATUS,
                "Quest Status"
        );
    }
}
