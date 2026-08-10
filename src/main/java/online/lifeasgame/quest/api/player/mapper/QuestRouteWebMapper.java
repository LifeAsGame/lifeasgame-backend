package online.lifeasgame.quest.api.player.mapper;

import online.lifeasgame.quest.api.player.request.QuestRouteRequest;
import online.lifeasgame.quest.api.player.response.QuestRouteResponse;
import online.lifeasgame.quest.application.result.QuestRouteResult;

public final class QuestRouteWebMapper {

    private QuestRouteWebMapper() {
    }

    public static Long toExpectedStepId(QuestRouteRequest.Advance request) {
        return request.expectedStepId();
    }

    public static QuestRouteResponse.Routes toRoutes(
            QuestRouteResult.Routes result
    ) {
        return new QuestRouteResponse.Routes(
                result.routes().stream()
                        .map(QuestRouteWebMapper::toRoute)
                        .toList()
        );
    }

    public static QuestRouteResponse.Route toRoute(
            QuestRouteResult.Route result
    ) {
        return new QuestRouteResponse.Route(
                result.id(),
                result.code(),
                result.definitionVersion(),
                result.title(),
                result.description(),
                result.primaryRoleTemplateCode(),
                toProgress(result.playerProgress()),
                result.steps().stream()
                        .map(QuestRouteWebMapper::toStep)
                        .toList()
        );
    }

    public static QuestRouteResponse.StepDetail toStepDetail(
            QuestRouteResult.StepDetail result
    ) {
        return new QuestRouteResponse.StepDetail(
                result.routeId(),
                result.routeCode(),
                toProgress(result.playerProgress()),
                toStep(result.step())
        );
    }

    private static QuestRouteResponse.PlayerProgress toProgress(
            QuestRouteResult.PlayerProgress result
    ) {
        if (result == null) return null;
        return new QuestRouteResponse.PlayerProgress(
                result.id(),
                result.currentStepId(),
                result.status(),
                result.selectedAt(),
                result.completedAt()
        );
    }

    private static QuestRouteResponse.Step toStep(
            QuestRouteResult.Step result
    ) {
        return new QuestRouteResponse.Step(
                result.id(),
                result.stepCode(),
                result.stepOrder(),
                result.title(),
                result.description(),
                result.criterionType(),
                result.requiredEvidenceCount(),
                result.userAdvanceRequired(),
                result.retroactiveEvidenceAllowed(),
                result.skipAllowed(),
                result.criteriaSatisfied(),
                result.state(),
                result.questLinks().stream()
                        .map(link -> new QuestRouteResponse.QuestLink(
                                link.questId(),
                                link.requirementType()
                        ))
                        .toList()
        );
    }
}
