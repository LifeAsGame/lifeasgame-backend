package online.lifeasgame.lifelog.quick.api;

import java.time.Instant;

public final class QuickRecordResponse {

    private QuickRecordResponse() {
    }

    public record Recorded(
            String sourceType,
            Long sourceId,
            Instant recordedAt,
            boolean replay
    ) {
    }
}
