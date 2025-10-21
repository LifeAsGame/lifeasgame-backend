package online.lifeasgame.lifelog.application.query;

import online.lifeasgame.lifelog.domain.MediaCategory;
import online.lifeasgame.lifelog.domain.MediaLog;
import online.lifeasgame.lifelog.domain.WatchStatus;

import java.util.List;

public interface MediaLogQueryRepository {
    List<MediaLog> findByPlayerId(Long playerId, int limit);

    List<MediaLog> search(
            Long playerId,
            MediaCategory category,
            WatchStatus status,
            String titleLike,
            int page,
            int size
    );
}
