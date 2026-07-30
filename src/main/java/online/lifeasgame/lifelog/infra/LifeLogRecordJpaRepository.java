package online.lifeasgame.lifelog.infra;

import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LifeLogRecordJpaRepository
        extends JpaRepository<LifeLogRecord, Long> {

    Optional<LifeLogRecord> findBySourceTypeAndSourceId(
            LifeLogSourceType sourceType,
            Long sourceId
    );
}
