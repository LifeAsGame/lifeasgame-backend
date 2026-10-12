package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.role.application.internal.RoleLookupApi;
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
    private final RoleLookupApi roleLookupApi;

    @Transactional
    public QuestRouteResult.Route select(Long routeId) {
        return select(routeId, null);
    }

    @Transactional
    public QuestRouteResult.Route select(Long routeId, Long roleId) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        QuestRoute route = routeReader.getRoute(routeId);
        boolean backendJourney = BackendJourneyAccess.ROUTE_CODE.equals(route.getCode());
        if (backendJourney) {
            if (roleId == null) throw new DomainException(QuestError.JOURNEY_ROLE_REQUIRED);
            var role = roleLookupApi.getOwnedActiveForUpdate(roleId, playerId);
            if (!BackendJourneyAccess.compatible(role.roleType())) {
                throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
            }
        } else if (roleId != null) {
            throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
        }
        Long firstStepId = route.firstStep().getId();
        playerQuestRouteRepository.insertIfAbsent(
                playerId,
                route.getId(),
                firstStepId,
                roleId,
                clock.instant()
        );
        PlayerQuestRoute playerRoute = routeReader.getPlayerRouteForUpdate(
                playerId,
                route.getId()
        );
        if (backendJourney && !roleId.equals(playerRoute.getRoleId())) {
            throw new DomainException(QuestError.JOURNEY_ROLE_MISMATCH);
        }
        return readModelFactory.create(playerId, route, playerRoute);
    }
}
