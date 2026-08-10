package online.lifeasgame.quest.api.player.response;

import java.time.Instant;
import java.util.List;

public final class QuestRouteResponse {

    private QuestRouteResponse() {
    }

    public record Routes(List<Route> routes) {
    }

    public record Route(
            Long id,
            String code,
            int definitionVersion,
            String title,
            String description,
            String primaryRoleTemplateCode,
            PlayerProgress playerProgress,
            List<Step> steps
    ) {
    }

    public record PlayerProgress(
            Long id,
            Long currentStepId,
            String status,
            Instant selectedAt,
            Instant completedAt
    ) {
    }

    public record Step(
            Long id,
            String stepCode,
            int stepOrder,
            String title,
            String description,
            String criterionType,
            int requiredEvidenceCount,
            boolean userAdvanceRequired,
            boolean retroactiveEvidenceAllowed,
            boolean skipAllowed,
            boolean criteriaSatisfied,
            String state,
            List<QuestLink> questLinks
    ) {
    }

    public record QuestLink(
            Long questId,
            String requirementType
    ) {
    }

    public record StepDetail(
            Long routeId,
            String routeCode,
            PlayerProgress playerProgress,
            Step step
    ) {
    }
}
