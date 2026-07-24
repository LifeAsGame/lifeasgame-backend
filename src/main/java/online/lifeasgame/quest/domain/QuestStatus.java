package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.util.List;
import java.util.Locale;

public enum QuestStatus {
    IN_PROGRESS,
    GOAL_REACHED,
    COMPLETED,
    CANCELED;

    public static QuestStatus parse(String raw) {
        if (isLegacyDone(raw)) {
            return COMPLETED;
        }
        return EnumParsers.parseStrict(
                QuestStatus.class,
                raw,
                QuestError.INVALID_QUEST_STATUS,
                "Quest Status"
        );
    }

    public static QuestStatus parseNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parse(raw);
    }

    public static List<QuestStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestStatus.class,
                raw == null ? null : raw.stream()
                        .map(value -> isLegacyDone(value) ? COMPLETED.name() : value)
                        .toList(),
                QuestError.INVALID_QUEST_STATUS,
                "Quest Status"
        );
    }

    private static boolean isLegacyDone(String raw) {
        return raw != null && "DONE".equals(raw.trim().toUpperCase(Locale.ROOT));
    }
}
