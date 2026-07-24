package online.lifeasgame.platform.outbox.application;

public record OutboxClaim(
        Long id,
        String eventId,
        String eventType,
        String payload,
        String lockedBy
) {
}
