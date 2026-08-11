package online.lifeasgame.lifelog.application.record;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogPeriodKey;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import online.lifeasgame.role.application.internal.RoleEventLookupApi;
import online.lifeasgame.role.application.internal.RoleLookupApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class LifeLogRecordRegistrar {

    private final LifeLogRecordRepository repository;
    private final PlayerTimezoneResolver timezoneResolver;
    private final RoleLookupApi roleLookupApi;
    private final RoleEventLookupApi roleEventLookupApi;
    private final Clock clock;

    public LifeLogRecord register(
            Long playerId,
            LifeLogSourceType sourceType,
            Long sourceId,
            LifeLogEntryMode entryMode,
            LifeLogRecordMetadataCommand metadata
    ) {
        LifeLogRecordMetadataCommand command =
                metadata == null
                        ? LifeLogRecordMetadataCommand.none()
                        : metadata;
        LifeLogRecordMetadataCommand.Resolved resolved = command.resolve();
        Instant occurredAt = clock.instant();
        RoleContext roleContext = resolveRoleContext(playerId, resolved);

        if (resolved.subtype() == null) {
            return repository.saveAndFlush(LifeLogRecord.legacy(
                    playerId,
                    sourceType,
                    sourceId,
                    entryMode,
                    roleContext.primaryRoleId(),
                    roleContext.roleEventId(),
                    occurredAt
            ));
        }

        LifeLogPeriodKey periodKey =
                resolved.reflectionScope()
                        == LifeLogReflectionScope.WEEKLY_LOOKBACK
                        ? LifeLogPeriodKey.weekly(
                                occurredAt,
                                timezoneResolver.resolve(playerId)
                        )
                        : null;
        return repository.saveAndFlush(LifeLogRecord.contentReady(
                playerId,
                sourceType,
                sourceId,
                resolved.subtype(),
                entryMode,
                resolved.reflectionScope(),
                periodKey,
                roleContext.primaryRoleId(),
                roleContext.roleEventId(),
                occurredAt
        ));
    }

    private RoleContext resolveRoleContext(
            Long playerId,
            LifeLogRecordMetadataCommand.Resolved metadata
    ) {
        Long roleId = metadata.primaryRoleId();
        Long eventId = metadata.roleEventId();
        if (eventId != null) {
            var event = roleEventLookupApi.getOwned(eventId, playerId);
            if (roleId != null && !roleId.equals(event.roleId())) {
                throw new DomainException(
                        LifeLogError.ROLE_EVENT_CONTEXT_MISMATCH
                );
            }
            roleId = event.roleId();
        }
        if (metadata.primaryRoleId() != null) {
            roleLookupApi.getOwned(metadata.primaryRoleId(), playerId);
        }
        return new RoleContext(roleId, eventId);
    }

    private record RoleContext(Long primaryRoleId, Long roleEventId) {
    }
}
