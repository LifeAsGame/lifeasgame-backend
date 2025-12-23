package online.lifeasgame.lifelog.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
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

    public MediaLog getByPlayerIdAndIdOrThrow(Long playerId, Long id) {
        return repository.findByIdAndPlayerId(id, playerId)
                .orElseThrow(() -> new DomainException(LifeLogError.MEDIA_NOT_FOUND));
    }

    public List<MediaLog> recent(Long playerId, int limit) {
        return repository.findByPlayerId(playerId, limit);
    }

    public List<MediaLog> search(
            Long playerId,
            String category,
            String status,
            String titleLike,
            int page,
            int size
    ) {
        MediaCategory mediaCategory = MediaCategory.parseNullable(category);
        WatchStatus watchStatus = WatchStatus.parseNullable(status);
        return repository.search(playerId, mediaCategory, watchStatus, titleLike, page, size);
    }
}
