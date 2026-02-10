package online.lifeasgame.quest.api.admin.mapper;

import online.lifeasgame.quest.api.admin.request.AdminQuestRequest;
import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.application.command.QuestCommand;
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
                result.rewardExp(),
                result.rewardStats(),
                result.dueAt()
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

    public static QuestCommand.Definition toDefinitionCommand(String questCode) {
        return new QuestCommand.Definition(questCode);
    }

    public static QuestCommand.UpdateDefinition toUpdateCommand(String questCode, AdminQuestRequest.Update request) {
        return new QuestCommand.UpdateDefinition(
                questCode,
                request.targetType(),
                request.targetValue(),
                request.rewardExp(),
                request.rewardStats(),
                request.repeatRule(),
                request.dueAt()
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
                result.rewardExp(),
                result.rewardStats(),
                result.dueAt()
        );
    }

    public static QuestCommand.Acceptances toAcceptancesCommand(String questCode, String status) {
        return new QuestCommand.Acceptances(questCode, status);
    }

    public static AdminQuestResponse.Acceptances toAcceptances(List<QuestResult.Acceptance> results) {
        return new AdminQuestResponse.Acceptances(
                results.stream()
                .map(AdminQuestWebMapper::toAcceptance)
                .toList());
    }

    public static QuestCommand.Acceptance toAcceptanceCommand(Long acceptanceId) {
        return new QuestCommand.Acceptance(acceptanceId);
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
                result.progress(),
                result.status(),
                result.repeatRule(),
                result.periodStart(),
                result.periodEnd(),
                result.completedAt(),
                result.dueAt()
        );
    }

    public static QuestCommand.AdjustProgress toAdjustProgressCommand(AdminQuestRequest.AdjustProgress request) {
        return new QuestCommand.AdjustProgress(
                request.delta()
        );
    }
}
