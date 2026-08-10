package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.quest.application.result.QuestRouteResult;
import online.lifeasgame.quest.domain.PlayerQuestRoute;
import online.lifeasgame.quest.domain.QuestRoute;
import online.lifeasgame.quest.domain.QuestRouteStep;
import online.lifeasgame.quest.domain.QuestRouteStepState;
import online.lifeasgame.quest.domain.error.QuestError;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
class QuestRouteReadModelFactory {

    private final QuestRouteCriteriaEvaluator criteriaEvaluator;

    QuestRouteResult.Route create(
            Long playerId,
            QuestRoute route,
            PlayerQuestRoute playerRoute
    ) {
        QuestRouteStep currentStep = playerRoute == null
                ? null
                : route.getStep(playerRoute.getCurrentStepId());
        List<QuestRouteResult.Step> steps = route.getSteps().stream()
                .map(step -> createStep(
                        playerId,
                        step,
                        playerRoute,
                        currentStep
                ))
                .toList();
        return new QuestRouteResult.Route(
                route.getId(),
                route.getCode(),
                route.getDefinitionVersion(),
                route.getTitle(),
                route.getDescription(),
                route.getPrimaryRoleTemplateCode(),
                playerRoute == null ? null : progress(playerRoute),
                steps
        );
    }

    QuestRouteResult.StepDetail createStepDetail(
            Long playerId,
            QuestRoute route,
            PlayerQuestRoute playerRoute,
            Long stepId
    ) {
        QuestRouteResult.Route model = create(playerId, route, playerRoute);
        QuestRouteResult.Step step = model.steps().stream()
                .filter(candidate -> candidate.id().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new DomainException(
                        QuestError.ROUTE_STEP_NOT_FOUND
                ));
        return new QuestRouteResult.StepDetail(
                route.getId(),
                route.getCode(),
                model.playerProgress(),
                step
        );
    }

    private QuestRouteResult.Step createStep(
            Long playerId,
            QuestRouteStep step,
            PlayerQuestRoute playerRoute,
            QuestRouteStep currentStep
    ) {
        boolean criteriaSatisfied = criteriaEvaluator.isSatisfied(
                playerId,
                step
        );
        QuestRouteStepState state = state(
                step,
                playerRoute,
                currentStep,
                criteriaSatisfied
        );
        List<QuestRouteResult.QuestLink> questLinks = step.getQuestLinks()
                .stream()
                .sorted(Comparator.comparing(link -> link.getQuestId()))
                .map(link -> new QuestRouteResult.QuestLink(
                        link.getQuestId(),
                        link.getRequirementType().name()
                ))
                .toList();
        return new QuestRouteResult.Step(
                step.getId(),
                step.getStepCode(),
                step.getStepOrder(),
                step.getTitle(),
                step.getDescription(),
                step.getCriterionType().name(),
                step.getRequiredEvidenceCount(),
                step.isUserAdvanceRequired(),
                step.isRetroactiveEvidenceAllowed(),
                step.isSkipAllowed(),
                criteriaSatisfied,
                state.name(),
                questLinks
        );
    }

    private QuestRouteStepState state(
            QuestRouteStep step,
            PlayerQuestRoute playerRoute,
            QuestRouteStep currentStep,
            boolean criteriaSatisfied
    ) {
        if (playerRoute == null) return QuestRouteStepState.LOCKED;
        if (playerRoute.isCompleted()) return QuestRouteStepState.COMPLETED;
        if (step.getStepOrder() < currentStep.getStepOrder()) {
            return QuestRouteStepState.COMPLETED;
        }
        if (step.getId().equals(currentStep.getId())) {
            return criteriaSatisfied
                    ? QuestRouteStepState.READY_TO_ADVANCE
                    : QuestRouteStepState.CURRENT;
        }
        return QuestRouteStepState.LOCKED;
    }

    private QuestRouteResult.PlayerProgress progress(
            PlayerQuestRoute playerRoute
    ) {
        return new QuestRouteResult.PlayerProgress(
                playerRoute.getId(),
                playerRoute.getCurrentStepId(),
                playerRoute.getStatus().name(),
                playerRoute.getSelectedAt(),
                playerRoute.getCompletedAt()
        );
    }
}
