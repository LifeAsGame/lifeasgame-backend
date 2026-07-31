package online.lifeasgame.quest.application.event;

import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class QuestCompletionEventFactory {

    public QuestEvent create(
            QuestAcceptance acceptance,
            Quest quest,
            String correlationId
    ) {
        return create(acceptance, quest, correlationId, Map.of());
    }

    public QuestEvent create(
            QuestAcceptance acceptance,
            Quest quest,
            String correlationId,
            Map<String, Object> contextAttributes
    ) {
        validate(acceptance, quest, correlationId);
        return QuestEvent.builder(QuestEventType.QUEST_COMPLETED)
                .attributes(contextAttributes)
                .questId(quest.getId())
                .questCode(quest.getCode())
                .playerId(acceptance.getPlayerId())
                .attribute("acceptanceId", acceptance.getId())
                .attribute("progress", acceptance.getProgressValue())
                .attribute("target", quest.target().value())
                .attribute("repeatRule", quest.getRepeatRule().name())
                .attribute(
                        "completionPolicy",
                        quest.getCompletionPolicy().name()
                )
                .attribute("goalReachedAt", acceptance.getGoalReachedAt())
                .attribute("completedAt", acceptance.getCompletedAt())
                .definitionSnapshot(quest)
                .occurredAt(acceptance.getCompletedAt())
                .correlationId(correlationId)
                .build();
    }

    private void validate(
            QuestAcceptance acceptance,
            Quest quest,
            String correlationId
    ) {
        require(acceptance != null, "acceptance must not be null");
        require(quest != null, "quest must not be null");
        require(
                acceptance.getId() != null && acceptance.getId() > 0,
                "acceptance must be persisted"
        );
        require(
                quest.getId() != null && quest.getId() > 0,
                "quest must be persisted"
        );
        require(acceptance.isCompleted(), "acceptance must be completed");
        require(
                acceptance.getGoalReachedAt() != null,
                "goalReachedAt must not be null"
        );
        require(
                acceptance.getCompletedAt() != null,
                "completedAt must not be null"
        );
        require(
                correlationId != null && !correlationId.isBlank(),
                "correlationId must not be blank"
        );
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
