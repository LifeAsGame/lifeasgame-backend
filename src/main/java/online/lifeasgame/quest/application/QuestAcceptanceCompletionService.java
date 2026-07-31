package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.event.QuestCompletionEventFactory;
import online.lifeasgame.quest.application.result.QuestResult;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.error.QuestError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class QuestAcceptanceCompletionService {

    private final QuestReader questReader;
    private final QuestWriter questWriter;
    private final DomainEventPublisher domainEventPublisher;
    private final QuestCompletionEventFactory completionEventFactory;
    private final Clock clock;

    @Transactional
    public QuestResult.Acceptance complete(Long acceptanceId) {
        return complete(null, acceptanceId);
    }

    @Transactional
    public QuestResult.Acceptance completeForPlayer(
            Long playerId,
            Long acceptanceId
    ) {
        return complete(playerId, acceptanceId);
    }

    private QuestResult.Acceptance complete(
            Long playerId,
            Long acceptanceId
    ) {
        QuestAcceptance acceptance =
                questReader.getAcceptanceForUpdate(acceptanceId);
        if (playerId != null && !playerId.equals(acceptance.getPlayerId())) {
            throw new DomainException(QuestError.QUEST_ACCEPTANCE_NOT_FOUND);
        }
        Quest quest = questReader.getById(acceptance.getQuestId());
        if (!quest.requiresUserConfirmation()) {
            throw new DomainException(QuestError.QUEST_COMPLETION_POLICY_NOT_USER_CONFIRM);
        }

        boolean changed = acceptance.complete(clock.instant());
        if (changed) {
            questWriter.saveAcceptance(acceptance);
            publishCompleted(acceptance, quest);
        }
        return QuestResult.Acceptance.from(acceptance, quest);
    }

    private void publishCompleted(QuestAcceptance acceptance, Quest quest) {
        domainEventPublisher.publish(
                completionEventFactory.create(
                        acceptance,
                        quest,
                        "quest:%d:acceptance:%d:completed".formatted(
                                quest.getId(),
                                acceptance.getId()
                        )
                )
        );
    }
}
