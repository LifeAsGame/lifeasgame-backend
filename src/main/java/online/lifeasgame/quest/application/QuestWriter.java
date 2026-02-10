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

import java.time.Instant;

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
                QuestEvent.builder(QuestEventType.QUEST_CREATED)
                        .questCode(saved.getCode())
                        .questId(saved.getId())
                        .attribute("title", saved.getTitle().value())
                        .attribute("category", saved.getCategory().name())
                        .attribute("targetType", saved.target().type().name())
                        .attribute("targetValue", saved.target().value())
                        .attribute("repeatRule", saved.getRepeatRule().name())
                        .attribute("rewardExp", saved.getReward().exp())
                        .attribute("rewardStats", saved.getReward().stats().stats())
                        .attribute("dueAt", saved.getDueAt())
                        .occurredAt(Instant.now())
                        .correlationId("quest:" + saved.getCode())
                        .build()
        );

        return saved;
    }

    public QuestAcceptance accept(QuestAcceptance questAcceptance) {
        return questAcceptanceRepository.save(questAcceptance);
    }
}
