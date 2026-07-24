package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;

import java.time.Instant;

public record LifeLogRecorded(
        String eventId,
        int eventVersion,
        Long playerId,
        Long lifeLogId,
        LifeLogType lifeLogType,
        Long primaryRoleId,
        Instant occurredAt
) implements DomainEvent {

    public static final int EVENT_VERSION = 1;

    public LifeLogRecorded {
        eventId = Guard.notBlank(eventId, "eventId");
        if (eventVersion != EVENT_VERSION) {
            throw new IllegalArgumentException(
                    "eventVersion must be " + EVENT_VERSION
            );
        }
        Guard.notNull(playerId, "playerId");
        Guard.notNull(lifeLogId, "lifeLogId");
        Guard.notNull(lifeLogType, "lifeLogType");
        if (primaryRoleId != null) {
            throw new IllegalArgumentException(
                    "primaryRoleId must be null until Role Domain is available"
            );
        }
        Guard.notNull(occurredAt, "occurredAt");
    }

    public static LifeLogRecorded of(
            String eventId,
            Long playerId,
            Long lifeLogId,
            LifeLogType lifeLogType,
            Instant occurredAt
    ) {
        return new LifeLogRecorded(
                eventId,
                EVENT_VERSION,
                playerId,
                lifeLogId,
                lifeLogType,
                null,
                occurredAt
        );
    }
}
