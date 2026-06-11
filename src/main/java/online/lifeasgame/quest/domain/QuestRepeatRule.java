package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Duration;
import java.util.List;

public enum QuestRepeatRule {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY;

    public static QuestRepeatRule parse(String raw) {
        return EnumParsers.parseStrict(
                QuestRepeatRule.class,
                raw,
                QuestError.INVALID_QUEST_REPEATABLE_RULE,
                "Quest repeatable rule"
        );
    }

    public static QuestRepeatRule parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<QuestRepeatRule> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                QuestRepeatRule.class,
                raw,
                QuestError.INVALID_QUEST_REPEATABLE_RULE,
                "Quest repeatable rules"
        );
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
