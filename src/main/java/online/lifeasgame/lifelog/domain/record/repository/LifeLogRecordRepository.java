package online.lifeasgame.lifelog.domain.record.repository;

import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;

import java.util.Optional;

public interface LifeLogRecordRepository {

    // Remove the Journal header only when its owned source was deleted in this transaction.
    void deleteBySourceAndPlayerId(LifeLogSourceType sourceType, Long sourceId, Long playerId);

    LifeLogRecord saveAndFlush(LifeLogRecord record);

    Optional<LifeLogRecord> findBySource(
            LifeLogSourceType sourceType,
            Long sourceId
    );
}
