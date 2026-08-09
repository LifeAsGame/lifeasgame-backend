package online.lifeasgame.quest.api.admin.mapper;

import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.query.QuestQuery;
import online.lifeasgame.quest.application.result.QuestResult;

import java.util.List;

public final class AdminQuestWebMapper {

    private AdminQuestWebMapper() {}

    public static AdminQuestResponse.Blueprints toBlueprints(List<QuestResult.Blueprint> results) {
        return new AdminQuestResponse.Blueprints(
                results.stream()
                        .map(AdminQuestWebMapper::toBlueprint)
                        .toList()
        );
    }

    private static AdminQuestResponse.Blueprint toBlueprint(QuestResult.Blueprint result) {
        return new AdminQuestResponse.Blueprint(
                result.code(),
                result.title(),
                result.category(),
                result.descriptionMd(),
                result.target().type().name(),
                result.target().value(),
                result.repeatRule(),
                result.completionPolicy(),
                result.definitionVersion(),
                result.rewardProfileCode(),
                result.rewardExp(),
                result.rewardStats(),
                result.dueAt(),
                result.semanticCategory(),
                result.progressSource(),
                result.repeatPolicy(),
                result.roleTemplateCode()
        );
    }

    public static AdminQuestResponse.Definitions toDefinitions(List<QuestResult.Definition> results) {
        return new AdminQuestResponse.Definitions(
                results.stream()
                        .map(AdminQuestWebMapper::toDefinition)
                        .toList());
    }

    public static QuestCommand.EnsureDefinition toEnsureCommand(String questCode) {
        return new QuestCommand.EnsureDefinition(questCode);
    }

    public static QuestQuery.Definition toDefinitionQuery(String questCode) {
        return new QuestQuery.Definition(questCode);
    }

    public static QuestCommand.UpdateDefinition toUpdateCommand(String questCode, AdminQuestRequest.Update request) {
        return new QuestCommand.UpdateDefinition(
                questCode,
                request.definitionVersion(),
                request.targetType(),
                request.targetValue(),
                request.rewardProfileCode(),
                request.rewardExp(),
                request.rewardStats(),
                request.repeatRule(),
                request.dueAt(),
                request.semanticCategory(),
                request.progressSource(),
                request.repeatPolicy(),
                request.roleTemplateCode()
        );
    }

    public static AdminQuestResponse.Definition toDefinition(QuestResult.Definition result) {
        return new AdminQuestResponse.Definition(
                result.id(),
                result.code(),
                result.title(),
                result.category(),
                result.descriptionMd(),
                result.targetType().name(),
                result.targetValue(),
                result.repeatRule(),
                result.completionPolicy(),
                result.definitionVersion(),
                result.rewardProfileCode(),
                result.rewardExp(),
                result.rewardStats(),
                result.dueAt(),
                result.semanticCategory(),
                result.progressSource(),
                result.repeatPolicy(),
                result.roleTemplateCode()
        );
    }

    public static QuestQuery.Acceptances toAcceptancesQuery(String questCode, String status) {
        return new QuestQuery.Acceptances(questCode, status);
    }

    public static AdminQuestResponse.Acceptances toAcceptances(List<QuestResult.Acceptance> results) {
        return new AdminQuestResponse.Acceptances(
                results.stream()
                .map(AdminQuestWebMapper::toAcceptance)
                .toList());
    }

    public static QuestQuery.Acceptance toAcceptanceQuery(Long acceptanceId) {
        return new QuestQuery.Acceptance(acceptanceId);
    }

    public static AdminQuestResponse.Acceptance toAcceptance(QuestResult.Acceptance result) {
        return new AdminQuestResponse.Acceptance(
                result.id(),
                result.questId(),
                result.playerId(),
                result.code(),
                result.title(),
                result.category(),
                result.targetType().name(),
                result.targetValue(),
                result.progressValue(),
                result.status(),
                result.completionPolicy(),
                result.repeatRule(),
                result.periodStart(),
                result.periodEnd(),
                result.acceptedAt(),
                result.periodKey(),
                result.goalReachedAt(),
                result.completedAt(),
                result.dueAt(),
                result.semanticCategory(),
                result.progressSource(),
                result.repeatPolicy(),
                result.roleTemplateCode()
        );
    }

    public static QuestCommand.AdjustProgress toAdjustProgressCommand(AdminQuestRequest.AdjustProgress request) {
        return new QuestCommand.AdjustProgress(
                request.delta()
        );
    }

    public static QuestCommand.ChangeStatus toChangeStatusCommand(AdminQuestRequest.ChangeStatus request) {
        return new QuestCommand.ChangeStatus(
                request.status(),
                request.reason()
        );
    }
}
