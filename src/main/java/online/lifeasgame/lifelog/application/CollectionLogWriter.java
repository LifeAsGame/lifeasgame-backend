package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.CollectionLog;
import online.lifeasgame.lifelog.domain.repository.CollectionLogRepository;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class CollectionLogWriter {

    private final CollectionLogRepository repository;
    private final LifeLogRecordRepository recordRepository;

    public CollectionLog create(CollectionLog collectionLog) {
        return repository.save(collectionLog);
    }

    public void delete(Long playerId, Long collectionId) {
        if (repository.deleteByIdAndPlayerId(collectionId, playerId) > 0) {
            recordRepository.deleteBySourceAndPlayerId(
                    LifeLogSourceType.COLLECTION, collectionId, playerId
            );
        }
    }
}
