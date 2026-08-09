package online.lifeasgame.quest.application.event;

import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class QuestTransitionEventFactory {

    public QuestEvent accepted(
            QuestAcceptance acceptance,
            Quest quest,
            Map<String, Object> context,
            Instant occurredAt,
            String correlationId
    ) {
        return base(
                QuestEventType.QUEST_ACCEPTED,
                acceptance,
                quest,
                context,
                occurredAt,
                correlationId
        ).build();
    }

    public QuestEvent progress(
            QuestAcceptance acceptance,
            Quest quest,
            Map<String, Object> context,
            Integer delta,
            Instant occurredAt,
            String correlationId
    ) {
        return base(
                QuestEventType.QUEST_PROGRESS,
                acceptance,
                quest,
                context,
                occurredAt,
                correlationId
        )
                .attribute("status", acceptance.getStatus().name())
                .attribute("delta", delta)
                .build();
    }

    public QuestEvent goalReached(
            QuestAcceptance acceptance,
            Quest quest,
            Map<String, Object> context,
            Instant occurredAt,
            String correlationId
    ) {
        return base(
                QuestEventType.QUEST_GOAL_REACHED,
                acceptance,
                quest,
                context,
                occurredAt,
                correlationId
        )
                .attribute("reachedAt", acceptance.getGoalReachedAt())
                .build();
    }

    private QuestEvent.Builder base(
            QuestEventType type,
            QuestAcceptance acceptance,
            Quest quest,
            Map<String, Object> context,
            Instant occurredAt,
            String correlationId
    ) {
        return QuestEvent.builder(type)
                .attributes(context)
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
                .occurredAt(occurredAt)
                .correlationId(correlationId);
    }
}
