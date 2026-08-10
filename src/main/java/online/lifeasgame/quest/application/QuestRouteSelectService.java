package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.QuestRoute;
import online.lifeasgame.quest.domain.repository.PlayerQuestRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class QuestRouteSelectService {

    private final QuestRouteReader routeReader;
    private final PlayerQuestRouteRepository playerQuestRouteRepository;
    private final QuestRouteReadModelFactory readModelFactory;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;

    @Transactional
    public QuestRouteResult.Route select(Long routeId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        QuestRoute route = routeReader.getRoute(routeId);
        Long firstStepId = route.firstStep().getId();
        playerQuestRouteRepository.insertIfAbsent(
                playerId,
                route.getId(),
                firstStepId,
                clock.instant()
        );
        PlayerQuestRoute playerRoute = routeReader.getPlayerRouteForUpdate(
                playerId,
                route.getId()
        );
        return readModelFactory.create(playerId, route, playerRoute);
    }
}
