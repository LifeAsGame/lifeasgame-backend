package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.repository.MediaLogRepository;
import online.lifeasgame.lifelog.domain.record.LifeLogSourceType;
import online.lifeasgame.lifelog.domain.record.repository.LifeLogRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class MediaLogWriter {

    private final MediaLogRepository repository;
    private final LifeLogRecordRepository recordRepository;

    public MediaLog create(MediaLog mediaLog) {
        return repository.save(mediaLog);
    }

    public void delete(Long playerId, Long mediaId) {
        if (repository.deleteByIdAndPlayerId(mediaId, playerId) > 0) {
            recordRepository.deleteBySourceAndPlayerId(
                    LifeLogSourceType.MEDIA, mediaId, playerId
            );
        }
    }
}
