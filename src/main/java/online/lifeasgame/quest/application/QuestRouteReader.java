package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.QuestRoute;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.repository.PlayerQuestRouteRepository;
import online.lifeasgame.quest.domain.repository.QuestRouteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
class QuestRouteReader {

    private final QuestRouteRepository questRouteRepository;
    private final PlayerQuestRouteRepository playerQuestRouteRepository;

    List<QuestRoute> findAllRoutes() {
        return questRouteRepository.findAll().stream()
                .peek(QuestRoute::validateDefinition)
                .toList();
    }

    QuestRoute getRoute(Long routeId) {
        QuestRoute route = questRouteRepository.findById(routeId)
                .orElseThrow(() -> new DomainException(
                        QuestError.ROUTE_NOT_FOUND
                ));
        route.validateDefinition();
        return route;
    }

    Optional<PlayerQuestRoute> findPlayerRoute(Long playerId, Long routeId) {
        return playerQuestRouteRepository.findByPlayerIdAndRouteId(
                playerId,
                routeId
        );
    }

    PlayerQuestRoute getPlayerRoute(Long playerId, Long routeId) {
        return findPlayerRoute(playerId, routeId)
                .orElseThrow(() -> new DomainException(
                        QuestError.PLAYER_ROUTE_NOT_FOUND
                ));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = false)
    PlayerQuestRoute getPlayerRouteForUpdate(Long playerId, Long routeId) {
        return playerQuestRouteRepository
                .findByPlayerIdAndRouteIdForUpdate(playerId, routeId)
                .orElseThrow(() -> new DomainException(
                        QuestError.PLAYER_ROUTE_NOT_FOUND
                ));
    }

    List<PlayerQuestRoute> findPlayerRoutes(Long playerId) {
        return playerQuestRouteRepository.findAllByPlayerId(playerId);
    }
}
