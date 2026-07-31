package online.lifeasgame.quest.application.trigger;

import java.time.Instant;

final class QuestSignalCorrelation {

    private QuestSignalCorrelation() {
    }

    static String sourceEvent(
            String sourceType,
            Long playerId,
            Object sourceId,
            Instant occurredAt
    ) {
        return "player:%d:%s:%s:%s".formatted(
                playerId,
                sourceType,
                sourceId,
                occurredAt
        );
    }

    static String lifeLog(Long lifeLogId) {
        return "lifelog:%d".formatted(lifeLogId);
    }
}
