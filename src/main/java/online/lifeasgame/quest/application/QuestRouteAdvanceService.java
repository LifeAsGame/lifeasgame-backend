package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.QuestRoute;
import online.lifeasgame.quest.domain.QuestRouteStep;
import online.lifeasgame.quest.domain.error.QuestError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class QuestRouteAdvanceService {

    private final QuestRouteReader routeReader;
    private final QuestRouteCriteriaEvaluator criteriaEvaluator;
    private final QuestRouteReadModelFactory readModelFactory;
    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final Clock clock;

    @Transactional
    public QuestRouteResult.Route advance(
            Long routeId,
            Long expectedStepId
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        PlayerQuestRoute playerRoute = routeReader.getPlayerRouteForUpdate(
                playerId,
                routeId
        );
        if (playerRoute.isCompleted()) {
            throw new DomainException(QuestError.ROUTE_ALREADY_COMPLETED);
        }
        QuestRoute route = routeReader.getRoute(routeId);
        QuestRouteStep currentStep = route.getStep(
                playerRoute.getCurrentStepId()
        );

        if (expectedStepId == null
                || !currentStep.getId().equals(expectedStepId)) {
            throw new DomainException(QuestError.ROUTE_STEP_NOT_CURRENT);
        }
        if (!criteriaEvaluator.isSatisfied(playerId, currentStep)) {
            throw new DomainException(
                    QuestError.ROUTE_STEP_CRITERIA_NOT_SATISFIED
            );
        }

        QuestRouteStep nextStep = route.nextStep(currentStep.getId());
        if (nextStep == null) {
            playerRoute.complete(expectedStepId, clock.instant());
        } else {
            playerRoute.advanceTo(expectedStepId, nextStep.getId());
        }
        return readModelFactory.create(playerId, route, playerRoute);
    }
}
