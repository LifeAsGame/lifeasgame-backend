package online.lifeasgame.lifelog.domain.record.repository;

import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;

import java.util.Optional;

public interface LifeLogRecordRepository {

    LifeLogRecord saveAndFlush(LifeLogRecord record);

    Optional<LifeLogRecord> findBySource(
            LifeLogSourceType sourceType,
            Long sourceId
    );
}
