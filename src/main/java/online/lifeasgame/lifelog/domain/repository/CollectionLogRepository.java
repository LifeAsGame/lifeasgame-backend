package online.lifeasgame.lifelog.domain.repository;

import online.lifeasgame.lifelog.domain.CollectionLog;

import java.util.Optional;

public interface CollectionLogRepository {
    CollectionLog save(CollectionLog log);
    Optional<CollectionLog> findById(Long id);
    Optional<CollectionLog> findByIdAndPlayerId(Long id, Long playerId);
}
