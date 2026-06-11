package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;

import java.util.List;
import java.util.Optional;

public interface MediaLogRepository {

    MediaLog save(MediaLog mediaLog);

    Optional<MediaLog> findById(Long id);

    Optional<MediaLog> findByIdAndPlayerId(Long id, Long playerId);

    List<MediaLog> findByPlayerId(Long playerId, int limit);

    List<MediaLog> search(
            Long playerId,
            MediaCategory category,
            WatchStatus status,
            String titleLike,
            int page,
            int size
    );

    void deleteByIdAndPlayerId(Long mediaId, Long playerId);
}
