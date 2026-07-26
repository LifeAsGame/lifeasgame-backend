package online.lifeasgame.quest.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.domain.Quest;
import online.lifeasgame.quest.domain.QuestAcceptance;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.domain.repository.QuestAcceptanceRepository;
import online.lifeasgame.quest.domain.repository.QuestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class QuestWriter {

    private final QuestRepository questRepository;
    private final QuestAcceptanceRepository questAcceptanceRepository;
    private final DomainEventPublisher domainEventPublisher;

    public Quest create(Quest quest) {
        Quest saved = questRepository.save(quest);

        domainEventPublisher.publish(
                QuestEvent.snapshot(
                        QuestEventType.QUEST_CREATED,
                        saved,
                        "quest:" + saved.getCode()
                )
        );

        return saved;
    }

    public QuestAcceptance accept(QuestAcceptance questAcceptance) {
        return questAcceptanceRepository.save(questAcceptance);
    }

    public QuestAcceptance saveAcceptance(QuestAcceptance questAcceptance) {
        return questAcceptanceRepository.save(questAcceptance);
    }
}
