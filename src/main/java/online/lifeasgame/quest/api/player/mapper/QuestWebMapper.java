package online.lifeasgame.quest.api.player.mapper;

import online.lifeasgame.quest.api.player.response.QuestResponse;
import online.lifeasgame.quest.application.result.QuestResult;

import java.util.List;

public final class QuestWebMapper {

    private QuestWebMapper() {}

    public static QuestResponse.Blueprint toResponse(QuestResult.Blueprint blueprint) {
        return QuestResponse.Blueprint.of(
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

    public static QuestResponse.Blueprints toBlueprints(List<QuestResult.Blueprint> blueprints) {
        return QuestResponse.Blueprints.of(blueprints.stream().map(QuestWebMapper::toResponse).toList());
    }

    public static QuestResponse.Acceptance toResponse(QuestResult.Acceptance acceptance) {
        return QuestResponse.Acceptance.of(
                acceptance.id(),
                acceptance.questId(),
                acceptance.code(),
                acceptance.title(),
                acceptance.category(),
                acceptance.descriptionMd(),
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

    public static QuestResponse.Acceptances toAcceptances(List<QuestResult.Acceptance> acceptances) {
        return QuestResponse.Acceptances.of(acceptances.stream().map(QuestWebMapper::toResponse).toList());
    }

    public static QuestResponse.PlayerQuest toPlayerQuest(QuestResult.PlayerQuest quest) {
        return QuestResponse.PlayerQuest.of(
                quest.code(),
                quest.title(),
                quest.category(),
                quest.descriptionMd(),
                quest.targetType().name(),
                quest.targetValue(),
                quest.repeatRule(),
                quest.rewardExp(),
                quest.rewardStats(),
                quest.dueAt(),
                quest.acceptance() == null ? null : toResponse(quest.acceptance())
        );
    }
}
