package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSubtype;

import java.time.Instant;

public record LifeLogRecorded(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        Long playerId,
        Long lifeLogId,
        Integer sourceDefinitionVersion,
        LifeLogSubtype subtype,
        LifeLogEntryMode entryMode,
        LifeLogReflectionScope reflectionScope,
        String periodKey,
        Long primaryRoleId,
        LifeLogType legacyLifeLogType
) implements DomainEvent {

    public static final String EVENT_TYPE = "LifeLogRecorded";
    public static final int EVENT_VERSION = 1;

    public LifeLogRecorded {
        eventId = Guard.notBlank(eventId, "eventId");
        if (!EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException(
                    "eventType must be " + EVENT_TYPE
            );
        }
        if (eventVersion != EVENT_VERSION) {
            throw new IllegalArgumentException(
                    "eventVersion must be " + EVENT_VERSION
            );
        }
        Guard.notNull(occurredAt, "occurredAt");
        positive(playerId, "playerId");
        positive(lifeLogId, "lifeLogId");
        if (primaryRoleId != null) {
            throw new IllegalArgumentException(
                    "primaryRoleId must be null until Role Domain is available"
            );
        }

        if (sourceDefinitionVersion == null) {
            if (subtype != null
                    || entryMode != null
                    || reflectionScope != null
                    || periodKey != null
                    || legacyLifeLogType == null) {
                throw new IllegalArgumentException(
                        "legacy Fact metadata contract is invalid"
                );
            }
        } else {
            if (sourceDefinitionVersion < 1) {
                throw new IllegalArgumentException(
                        "sourceDefinitionVersion must be at least 1"
                );
            }
            Guard.notNull(entryMode, "entryMode");
            if (legacyLifeLogType != null) {
                throw new IllegalArgumentException(
                        "physical source type is forbidden in new Fact"
                );
            }
            LifeLogRecord.validateReflection(
                    subtype,
                    reflectionScope,
                    periodKey
            );
        }
    }

    public static LifeLogRecorded from(
            String eventId,
            LifeLogRecord record
    ) {
        Guard.notNull(record, "record");
        return new LifeLogRecorded(
                eventId,
                EVENT_TYPE,
                EVENT_VERSION,
                record.getOccurredAt(),
                record.getPlayerId(),
                record.getId(),
                record.getSourceDefinitionVersion(),
                record.getSubtype(),
                record.getEntryMode(),
                record.getReflectionScope(),
                record.getPeriodKey(),
                record.getPrimaryRoleId(),
                null
        );
    }

    public static LifeLogRecorded legacy(
            String eventId,
            int eventVersion,
            Long playerId,
            Long lifeLogId,
            LifeLogType lifeLogType,
            Long primaryRoleId,
            Instant occurredAt
    ) {
        return new LifeLogRecorded(
                eventId,
                EVENT_TYPE,
                eventVersion,
                occurredAt,
                playerId,
                lifeLogId,
                null,
                null,
                null,
                null,
                null,
                primaryRoleId,
                lifeLogType
        );
    }

    public boolean isContentReady() {
        return sourceDefinitionVersion != null
                && subtype != null
                && entryMode != null;
    }

    public LifeLogRecorded requireContentReady() {
        if (!isContentReady()) {
            throw new IllegalStateException(
                    "LifeLogRecorded is not content-ready"
            );
        }
        return this;
    }

    private static void positive(Long value, String name) {
        Guard.notNull(value, name);
        Guard.minValue(value, 1L, name);
    }
}
