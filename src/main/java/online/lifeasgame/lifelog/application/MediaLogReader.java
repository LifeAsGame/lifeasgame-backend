package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.lifelog.application.query.MediaLogQueryRepository;
import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;
import online.lifeasgame.lifelog.domain.error.LifeLogError;
import online.lifeasgame.lifelog.domain.repository.MediaLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class MediaLogReader {

    private final MediaLogRepository repository;
    private final MediaLogQueryRepository queryRepository;

    public MediaLog getMediaLog(Long playerId, Long id) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(LifeLogError.MEDIA_NOT_FOUND));
    }

    public List<MediaLog> recent(Long playerId, int limit) {
        return queryRepository.findByPlayerId(playerId, limit);
    }

    public List<MediaLog> search(Long playerId, String category, String status, String titleLike, int page, int size) {
        MediaCategory parsedCategory = MediaCategory.parseNullable(category);
        WatchStatus parsedStatus = WatchStatus.parseNullable(status);
        return queryRepository.search(playerId, parsedCategory, parsedStatus, titleLike, page, size);
    }
}
