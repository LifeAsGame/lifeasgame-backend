package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.error.QuestError;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class QuestAcceptanceCompletionService {

    private final QuestReader questReader;
    private final QuestWriter questWriter;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public QuestResult.Acceptance complete(Long acceptanceId) {
        QuestAcceptance acceptance = questReader.getAcceptance(acceptanceId);
        Quest quest = questReader.getById(acceptance.getQuestId());
        if (!quest.requiresUserConfirmation()) {
            throw new DomainException(QuestError.QUEST_COMPLETION_POLICY_NOT_USER_CONFIRM);
        }

        boolean changed = acceptance.complete(Instant.now());
        if (changed) {
            questWriter.saveAcceptance(acceptance);
            publishCompleted(acceptance, quest);
        }
        return QuestResult.Acceptance.from(acceptance, quest);
    }

    private void publishCompleted(QuestAcceptance acceptance, Quest quest) {
        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_COMPLETED)
                        .questId(quest.getId())
                        .questCode(quest.getCode())
                        .playerId(acceptance.getPlayerId())
                        .attribute("acceptanceId", acceptance.getId())
                        .attribute("progress", acceptance.getProgressValue())
                        .attribute("target", quest.target().value())
                        .attribute("goalReachedAt", acceptance.getGoalReachedAt())
                        .attribute("completedAt", acceptance.getCompletedAt())
                        .attribute("completionPolicy", quest.getCompletionPolicy().name())
                        .occurredAt(acceptance.getCompletedAt())
                        .correlationId(
                                "quest:%d:acceptance:%d:completed".formatted(
                                        quest.getId(),
                                        acceptance.getId()
                                )
                        )
                        .build()
        );
    }
}
