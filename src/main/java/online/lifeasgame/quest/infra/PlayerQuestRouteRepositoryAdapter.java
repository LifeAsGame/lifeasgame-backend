package online.lifeasgame.quest.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.repository.PlayerQuestRouteRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PlayerQuestRouteRepositoryAdapter
        implements PlayerQuestRouteRepository {

    private final JpaPlayerQuestRouteRepository jpaRepository;

    @Override
    public void insertIfAbsent(
            Long playerId,
            Long routeId,
            Long firstStepId,
            Instant selectedAt
    ) {
        jpaRepository.insertIfAbsent(
                playerId,
                routeId,
                firstStepId,
                selectedAt
        );
    }

    @Override
    public Optional<PlayerQuestRoute> findByPlayerIdAndRouteId(
            Long playerId,
            Long routeId
    ) {
        return jpaRepository.findByPlayerIdAndRouteId(playerId, routeId);
    }

    @Override
    public Optional<PlayerQuestRoute> findByPlayerIdAndRouteIdForUpdate(
            Long playerId,
            Long routeId
    ) {
        return jpaRepository.findByPlayerIdAndRouteIdForUpdate(
                playerId,
                routeId
        );
    }

    @Override
    public List<PlayerQuestRoute> findAllByPlayerId(Long playerId) {
        return jpaRepository.findAllByPlayerId(playerId);
    }
}
