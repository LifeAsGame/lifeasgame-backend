package online.lifeasgame.quest.api.admin.mapper;

import online.lifeasgame.quest.api.admin.response.AdminQuestResponse;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;

import java.util.List;

public final class AdminQuestWebMapper {

    private AdminQuestWebMapper() {}

    public static AdminQuestResponse.Blueprints toBlueprints(List<QuestResult.Blueprint> blueprints) {
        return AdminQuestResponse.Blueprints.of(blueprints.stream()
                .map(AdminQuestWebMapper::toBlueprint)
                .toList());
    }

    public static AdminQuestResponse.Definitions toDefinitions(List<QuestResult.Definition> definitions) {
        return AdminQuestResponse.Definitions.of(definitions.stream()
                .map(AdminQuestWebMapper::toDefinition)
                .toList());
    }

    public static QuestCommand.EnsureDefinition toEnsureCommand(String questCode) {
        return new QuestCommand.EnsureDefinition(questCode);
    }

    public static QuestCommand.UpdateDefinition toUpdateCommand(String questCode, online.lifeasgame.quest.api.admin.request.AdminQuestRequest.Update request) {
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

    public static QuestCommand.Definition toDefinitionCommand(String questCode) {
        return new QuestCommand.Definition(questCode);
    }

    public static QuestCommand.Acceptances toAcceptancesCommand(String questCode, online.lifeasgame.quest.domain.QuestStatus status) {
        return new QuestCommand.Acceptances(questCode, status);
    }

    public static QuestCommand.Acceptance toAcceptanceCommand(Long acceptanceId) {
        return new QuestCommand.Acceptance(acceptanceId);
    }

    public static AdminQuestResponse.Definition toDefinition(QuestResult.Definition definition) {
        return AdminQuestResponse.Definition.of(
                definition.id(),
                definition.code(),
                definition.title(),
                definition.category(),
                definition.descriptionMd(),
                definition.targetType().name(),
                definition.targetValue(),
                definition.repeatRule(),
                definition.rewardExp(),
                definition.rewardStats(),
                definition.dueAt()
        );
    }

    public static AdminQuestResponse.Acceptances toAcceptances(List<QuestResult.Acceptance> acceptances) {
        return AdminQuestResponse.Acceptances.of(acceptances.stream()
                .map(AdminQuestWebMapper::toAcceptance)
                .toList());
    }

    public static AdminQuestResponse.Acceptance toAcceptance(QuestResult.Acceptance acceptance) {
        return AdminQuestResponse.Acceptance.of(
                acceptance.id(),
                acceptance.questId(),
                acceptance.playerId(),
                acceptance.code(),
                acceptance.title(),
                acceptance.category(),
                acceptance.targetType().name(),
                acceptance.targetValue(),
                acceptance.progress(),
                acceptance.status(),
                acceptance.repeatRule(),
                acceptance.periodStart(),
                acceptance.periodEnd(),
                acceptance.completedAt(),
                acceptance.dueAt()
        );
    }

    private static AdminQuestResponse.Blueprint toBlueprint(QuestResult.Blueprint blueprint) {
        return AdminQuestResponse.Blueprint.of(
                blueprint.code(),
                blueprint.title(),
                blueprint.category(),
                blueprint.descriptionMd(),
                blueprint.target().type().name(),
                blueprint.target().value(),
                blueprint.repeatRule(),
                blueprint.rewardExp(),
                blueprint.rewardStats(),
                blueprint.dueAt()
        );
    }
}
