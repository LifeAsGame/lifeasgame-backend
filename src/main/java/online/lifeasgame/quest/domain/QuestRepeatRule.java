package online.lifeasgame.quest.domain;

import java.time.Duration;
import java.util.Locale;

public enum QuestRepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    public static QuestRepeatRule parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        try {
            return QuestRepeatRule.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid repeatRule: " + raw);
        }
    }

    public static QuestRepeatRule parseNullable(String raw) {
        return (raw == null || raw.isBlank()) ? null : parse(raw);
    }

    public Duration idempotencyTtl() {
        return switch (this) {
            case NONE -> Duration.ofDays(90);
            case DAILY -> Duration.ofDays(7);
            case WEEKLY -> Duration.ofDays(30);
            case MONTHLY -> Duration.ofDays(120);
        };
    }
}
