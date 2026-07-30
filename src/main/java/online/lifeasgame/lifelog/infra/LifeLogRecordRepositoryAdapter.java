package online.lifeasgame.lifelog.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.record.LifeLogRecord;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LifeLogRecordRepositoryAdapter
        implements LifeLogRecordRepository {

    private final LifeLogRecordJpaRepository jpaRepository;

    @Override
    public LifeLogRecord saveAndFlush(LifeLogRecord record) {
        return jpaRepository.saveAndFlush(record);
    }

    @Override
    public Optional<LifeLogRecord> findBySource(
            LifeLogSourceType sourceType,
            Long sourceId
    ) {
        return jpaRepository.findBySourceTypeAndSourceId(
                sourceType,
                sourceId
        );
    }
}
