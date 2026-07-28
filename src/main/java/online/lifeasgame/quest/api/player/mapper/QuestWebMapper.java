package online.lifeasgame.quest.api.player.mapper;

import online.lifeasgame.quest.api.player.request.QuestRequest;
import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.application.command.QuestCommand;
import online.lifeasgame.quest.application.result.QuestResult;

import java.util.List;

public final class QuestWebMapper {

    private QuestWebMapper() {}

    public static QuestCommand.PlayerQuests toListCommand(String status) {
        return new QuestCommand.PlayerQuests(status);
    }

    public static QuestResponse.Acceptances toAcceptances(List<QuestResult.Acceptance> results) {
        return new QuestResponse.Acceptances(
                results.stream()
                        .map(QuestWebMapper::toAcceptance)
                        .toList()
        );
    }

    public static QuestCommand.PlayerQuest toPlayerQuestCommand(String questCode) {
        return new QuestCommand.PlayerQuest(questCode);
    }

    public static QuestResponse.PlayerQuest toPlayerQuest(QuestResult.PlayerQuest result) {
        return new QuestResponse.PlayerQuest(
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
                result.acceptance() == null
                        ? null
                        : toAcceptance(result.acceptance()),
                result.semanticCategory(),
                result.progressSource(),
                result.repeatPolicy(),
                result.roleTemplateCode()
        );
    }

    public static QuestResponse.Acceptance toAcceptance(QuestResult.Acceptance result) {
        return new QuestResponse.Acceptance(
                result.id(),
                result.questId(),
                result.code(),
                result.title(),
                result.category(),
                result.descriptionMd(),
                result.targetType().name(),
                result.targetValue(),
                result.progressValue(),
                result.status(),
                result.completionPolicy(),
                result.repeatRule(),
                result.periodStart(),
                result.periodEnd(),
                result.goalReachedAt(),
                result.completedAt(),
                result.dueAt(),
                result.semanticCategory(),
                result.progressSource(),
                result.repeatPolicy(),
                result.roleTemplateCode()
        );
    }

    public static QuestResponse.Blueprint toBlueprint(QuestResult.Blueprint result) {
        return new QuestResponse.Blueprint(
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

    public static QuestResponse.Blueprints toBlueprints(List<QuestResult.Blueprint> results) {
        return new QuestResponse.Blueprints(
                results.stream()
                        .map(QuestWebMapper::toBlueprint)
                        .toList()
        );
    }

    public static QuestCommand.Accept toAcceptCommand(String questCode, QuestRequest.Accept request) {
        return new QuestCommand.Accept(
                questCode,
                request.partyId(),
                request.guildId()
        );
    }

    public static QuestCommand.Cancel toCancelCommand(String questCode, QuestRequest.Cancel request) {
        return new QuestCommand.Cancel(
                questCode,
                request.reason()
        );
    }

    public static QuestResponse.Canceled toCanceled(QuestResult.Canceled result) {
        return new QuestResponse.Canceled(
                result.playerId(),
                result.questId(),
                result.questCode()
        );
    }
}
