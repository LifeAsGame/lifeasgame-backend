package online.lifeasgame.lifelog.domain.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "life_log_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_life_log_record_source",
                columnNames = {"source_type", "source_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LifeLogRecord extends AbstractTime {

    public static final int SOURCE_DEFINITION_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 20,
            updatable = false
    )
    private LifeLogSourceType sourceType;

    @Column(name = "source_id", nullable = false, updatable = false)
    private Long sourceId;

    @Column(
            name = "source_definition_version",
            nullable = false,
            updatable = false
    )
    private int sourceDefinitionVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtype", length = 20, updatable = false)
    private LifeLogSubtype subtype;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "entry_mode",
            nullable = false,
            length = 20,
            updatable = false
    )
    private LifeLogEntryMode entryMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reflection_scope", length = 30, updatable = false)
    private LifeLogReflectionScope reflectionScope;

    @Column(name = "period_key", length = 20, updatable = false)
    private String periodKey;

    @Column(name = "primary_role_id", updatable = false)
    private Long primaryRoleId;

    @Column(name = "role_event_id", updatable = false)
    private Long roleEventId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    private LifeLogRecord(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            int sourceDefinitionVersion,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            String periodKey,
            Long primaryRoleId,
            Long roleEventId,
            Instant occurredAt
    ) {
        this.playerId = positive(playerId, "playerId");
        this.sourceType = Guard.notNull(sourceType, "sourceType");
        this.sourceId = positive(sourceId, "sourceId");
        if (sourceDefinitionVersion < 1) {
            throw new IllegalArgumentException(
                    "sourceDefinitionVersion must be at least 1"
            );
        }
        this.sourceDefinitionVersion = sourceDefinitionVersion;
        this.subtype = subtype;
        this.entryMode = Guard.notNull(entryMode, "entryMode");
        validateReflection(subtype, reflectionScope, periodKey);
        this.reflectionScope = reflectionScope;
        this.periodKey = periodKey;
        this.primaryRoleId = nullablePositive(primaryRoleId, "primaryRoleId");
        this.roleEventId = nullablePositive(roleEventId, "roleEventId");
        if (this.roleEventId != null && this.primaryRoleId == null) {
            throw new IllegalArgumentException(
                    "roleEventId requires primaryRoleId"
            );
        }
        this.occurredAt = Guard.notNull(occurredAt, "occurredAt");
    }

    public static LifeLogRecord legacy(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogEntryMode entryMode,
            Instant occurredAt
    ) {
        return new LifeLogRecord(
                playerId,
                sourceType,
                sourceId,
                SOURCE_DEFINITION_VERSION,
                null,
                entryMode,
                null,
                null,
                null,
                null,
                occurredAt
        );
    }

    public static LifeLogRecord legacy(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogEntryMode entryMode,
            Long primaryRoleId,
            Long roleEventId,
            Instant occurredAt
    ) {
        return new LifeLogRecord(
                playerId,
                sourceType,
                sourceId,
                SOURCE_DEFINITION_VERSION,
                null,
                entryMode,
                null,
                null,
                primaryRoleId,
                roleEventId,
                occurredAt
        );
    }

    public static LifeLogRecord contentReady(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            LifeLogPeriodKey periodKey,
            Instant occurredAt
    ) {
        return new LifeLogRecord(
                playerId,
                sourceType,
                sourceId,
                SOURCE_DEFINITION_VERSION,
                Guard.notNull(subtype, "subtype"),
                entryMode,
                reflectionScope,
                periodKey == null ? null : periodKey.value(),
                null,
                null,
                occurredAt
        );
    }

    public static LifeLogRecord contentReady(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogSubtype subtype,
            LifeLogEntryMode entryMode,
            LifeLogReflectionScope reflectionScope,
            LifeLogPeriodKey periodKey,
            Long primaryRoleId,
            Long roleEventId,
            Instant occurredAt
    ) {
        return new LifeLogRecord(
                playerId,
                sourceType,
                sourceId,
                SOURCE_DEFINITION_VERSION,
                Guard.notNull(subtype, "subtype"),
                entryMode,
                reflectionScope,
                periodKey == null ? null : periodKey.value(),
                primaryRoleId,
                roleEventId,
                occurredAt
        );
    }

    public boolean isContentReady() {
        return subtype != null;
    }

    private static Long positive(Long value, String name) {
        Guard.notNull(value, name);
        Guard.minValue(value, 1L, name);
        return value;
    }

    private static Long nullablePositive(Long value, String name) {
        return value == null ? null : positive(value, name);
    }

    public static void validateReflection(
            LifeLogSubtype subtype,
            LifeLogReflectionScope reflectionScope,
            String periodKey
    ) {
        if (subtype != LifeLogSubtype.REFLECTION) {
            if (reflectionScope != null || periodKey != null) {
                throw new IllegalArgumentException(
                        "reflection metadata requires REFLECTION subtype"
                );
            }
            return;
        }
        if (reflectionScope == null) {
            if (periodKey != null) {
                throw new IllegalArgumentException(
                        "periodKey requires reflectionScope"
                );
            }
            return;
        }
        if (reflectionScope != LifeLogReflectionScope.WEEKLY_LOOKBACK
                || periodKey == null) {
            throw new IllegalArgumentException(
                    "WEEKLY_LOOKBACK requires periodKey"
            );
        }
        new LifeLogPeriodKey(periodKey);
    }
}
