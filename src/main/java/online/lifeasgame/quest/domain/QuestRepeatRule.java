package online.lifeasgame.quest.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.quest.domain.error.QuestError;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public enum QuestRepeatRule {
    ONCE,
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

    public static QuestRepeatRule parsePolicy(String raw) {
        QuestRepeatRule parsed = EnumParsers.parseStrict(
                QuestRepeatRule.class,
                raw,
                QuestError.INVALID_QUEST_REPEAT_POLICY,
                "Quest repeat policy"
        );
        if (!parsed.isFinalPolicy()) {
            throw new online.lifeasgame.core.error.DomainException(
                    QuestError.INVALID_QUEST_REPEAT_POLICY
            );
        }
        return parsed;
    }

    public static QuestRepeatRule parsePolicyNullable(String raw) {
        return raw == null ? null : parsePolicy(raw);
    }

    public boolean isFinalPolicy() {
        return this == ONCE || this == DAILY || this == WEEKLY;
    }

    public boolean isOneTime() {
        return this == ONCE || this == NONE;
    }

    public TimePeriod periodFor(LocalDate eventDate) {
        return switch (this) {
            case ONCE, NONE -> TimePeriod.forever();
            case DAILY -> TimePeriod.daily(eventDate);
            case WEEKLY -> TimePeriod.weekly(eventDate);
            case MONTHLY -> TimePeriod.monthly(eventDate);
        };
    }

    public Duration idempotencyTtl() {
        return switch (this) {
            case ONCE, NONE -> Duration.ofDays(90);
            case DAILY -> Duration.ofDays(7);
            case WEEKLY -> Duration.ofDays(30);
            case MONTHLY -> Duration.ofDays(120);
        };
    }
}
