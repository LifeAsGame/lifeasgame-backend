package online.lifeasgame.quest.domain.repository;

import online.lifeasgame.quest.domain.PlayerQuestRoute;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PlayerQuestRouteRepository {
    void insertIfAbsent(
            Long playerId,
            Long routeId,
            Long firstStepId,
            Instant selectedAt
    );

    Optional<PlayerQuestRoute> findByPlayerIdAndRouteId(
            Long playerId,
            Long routeId
    );

    Optional<PlayerQuestRoute> findByPlayerIdAndRouteIdForUpdate(
            Long playerId,
            Long routeId
    );

    List<PlayerQuestRoute> findAllByPlayerId(Long playerId);
}
