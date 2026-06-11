package online.lifeasgame.quest.application.automation;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.idempotency.IdempotencyKeyStore;
import online.lifeasgame.quest.domain.Quest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
class QuestSignalIdempotencyGuard {

    private static final Duration DEFAULT_TTL = Duration.ofDays(90);
    private static final Duration MINIMUM_TTL = Duration.ofHours(1);

    private final IdempotencyKeyStore idempotencyKeyStore;

    boolean accept(Quest quest, QuestSignal signal) {
        String correlationId = signal.correlationId();
        if (correlationId == null || correlationId.isBlank()) {
            return true;
        }

        Duration ttl = computeTtl(quest);
        return idempotencyKeyStore.acquire(keyFor(signal, correlationId), ttl);
    }

    private Duration computeTtl(Quest quest) {
        Duration ttl = quest.getRepeatRule() == null ? DEFAULT_TTL : quest.getRepeatRule().idempotencyTtl();

        Instant dueAt = quest.getDueAt();
        if (dueAt != null) {
            Duration untilDue = Duration.between(Instant.now(), dueAt.plus(1, ChronoUnit.DAYS));
            if (!untilDue.isNegative() && (ttl == null || untilDue.compareTo(ttl) < 0)) {
                ttl = untilDue;
            }
        }

        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            ttl = DEFAULT_TTL;
        }

        if (ttl.compareTo(MINIMUM_TTL) < 0) {
            ttl = MINIMUM_TTL;
        }

        return ttl;
    }

    private String keyFor(QuestSignal signal, String correlationId) {
        Guard.notNull(signal.questCode(), "questCode");
        Guard.notNull(signal.playerId(), "playerId");
        return "quest:signal:%s:%d:%s".formatted(signal.questCode().value(), signal.playerId(), correlationId);
    }
}
