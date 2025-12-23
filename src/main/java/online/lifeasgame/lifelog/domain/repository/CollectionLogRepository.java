package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.CollectionCategory;
import online.lifeasgame.lifelog.domain.CollectionLog;

import java.util.List;
import java.util.Optional;

public interface CollectionLogRepository {
    CollectionLog save(CollectionLog log);

    Optional<CollectionLog> findById(Long id);

    Optional<CollectionLog> findByIdAndPlayerId(Long id, Long playerId);

    List<CollectionLog> findByPlayerId(Long playerId, int limit);

    List<CollectionLog> search(Long playerId, CollectionCategory category, String titleLike, int page, int size);
}
