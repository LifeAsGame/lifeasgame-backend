package online.lifeasgame.quest.domain;

import java.time.Duration;

public enum QuestRepeatRule {
    NONE(Duration.ofDays(365)),
    DAILY(Duration.ofDays(3)),
    WEEKLY(Duration.ofDays(21)),
    MONTHLY(Duration.ofDays(93));

    private final Duration idempotencyTtl;

    QuestRepeatRule(Duration idempotencyTtl) {
        this.idempotencyTtl = idempotencyTtl;
    }

    public Duration idempotencyTtl() {
        return idempotencyTtl;
    }
}
