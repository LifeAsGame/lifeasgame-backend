package online.lifeasgame.platform.outbox.application.codec;

import java.time.Instant;

public record OutboxEventEnvelope(
        String eventType,
        String payload,
        Instant occurredAt
) {
}
