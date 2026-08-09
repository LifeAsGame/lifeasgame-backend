package online.lifeasgame.lifelog.application.record;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.record.LifeLogEntryMode;
import online.lifeasgame.lifelog.domain.record.LifeLogPeriodKey;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogReflectionScope;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
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

        if (resolved.subtype() == null) {
            return repository.saveAndFlush(LifeLogRecord.legacy(
                    playerId,
                    sourceType,
                    sourceId,
                    entryMode,
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
                occurredAt
        ));
    }
}
