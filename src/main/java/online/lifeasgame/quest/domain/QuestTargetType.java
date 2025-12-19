package online.lifeasgame.quest.domain;

import java.util.Locale;

public enum QuestTargetType {
    MINUTES, COUNT, SCORE;

    public static QuestTargetType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("targetType is required");
        }
        try {
            return QuestTargetType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid targetType: " + raw);
        }
    }

    public static QuestTargetType parseNullable(String raw) {
        return (raw == null || raw.isBlank()) ? null : parse(raw);
    }
}
