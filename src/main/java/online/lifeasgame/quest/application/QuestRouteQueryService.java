package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.QuestRoute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestRouteQueryService {

    private final QuestRouteReader routeReader;
    private final QuestRouteReadModelFactory readModelFactory;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    public QuestRouteResult.Routes routes() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        Map<Long, PlayerQuestRoute> playerRoutes = routeReader
                .findPlayerRoutes(playerId)
                .stream()
                .collect(Collectors.toMap(
                        PlayerQuestRoute::getRouteId,
                        Function.identity()
                ));
        return new QuestRouteResult.Routes(
                routeReader.findAllRoutes().stream()
                        .map(route -> readModelFactory.create(
                                playerId,
                                route,
                                playerRoutes.get(route.getId())
                        ))
                        .toList()
        );
    }

    public QuestRouteResult.Route route(Long routeId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        QuestRoute route = routeReader.getRoute(routeId);
        PlayerQuestRoute playerRoute = routeReader
                .findPlayerRoute(playerId, routeId)
                .orElse(null);
        return readModelFactory.create(playerId, route, playerRoute);
    }

    public QuestRouteResult.Routes myRoutes() {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        return new QuestRouteResult.Routes(
                routeReader.findPlayerRoutes(playerId).stream()
                        .map(playerRoute -> readModelFactory.create(
                                playerId,
                                routeReader.getRoute(playerRoute.getRouteId()),
                                playerRoute
                        ))
                        .toList()
        );
    }

    public QuestRouteResult.Route myRoute(Long routeId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        PlayerQuestRoute playerRoute = routeReader.getPlayerRoute(
                playerId,
                routeId
        );
        return readModelFactory.create(
                playerId,
                routeReader.getRoute(routeId),
                playerRoute
        );
    }

    public QuestRouteResult.StepDetail myStep(Long routeId, Long stepId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        PlayerQuestRoute playerRoute = routeReader.getPlayerRoute(
                playerId,
                routeId
        );
        QuestRoute route = routeReader.getRoute(routeId);
        route.getStep(stepId);
        return readModelFactory.createStepDetail(
                playerId,
                route,
                playerRoute,
                stepId
        );
    }
}
