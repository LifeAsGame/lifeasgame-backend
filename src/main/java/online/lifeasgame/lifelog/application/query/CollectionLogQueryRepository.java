package online.lifeasgame.lifelog.application.query;

import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;

import java.util.List;

public interface CollectionLogQueryRepository {
    List<CollectionLog> findByPlayer(Long playerId, int limit);

    List<CollectionLog> search(Long playerId, CollectionCategory category, String titleLike, int page, int size);
}
