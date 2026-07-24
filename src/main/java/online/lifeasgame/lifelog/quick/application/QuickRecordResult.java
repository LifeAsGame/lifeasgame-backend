package online.lifeasgame.lifelog.quick.application;

import online.lifeasgame.lifelog.domain.event.LifeLogType;
import online.lifeasgame.lifelog.quick.domain.QuickRecordRequestReceipt;

import java.time.Instant;

public final class QuickRecordResult {

    private QuickRecordResult() {
    }

    public record Recorded(
            LifeLogType sourceType,
            Long sourceId,
            Instant recordedAt,
            boolean replay
    ) {
        public static Recorded from(
                QuickRecordRequestReceipt.StoredResult result,
                boolean replay
        ) {
            return new Recorded(
                    result.sourceType(),
                    result.sourceId(),
                    result.recordedAt(),
                    replay
            );
        }
    }
}
