package online.lifeasgame.quest.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestRewardReadyPublisher {

    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onQuestEvent(QuestEvent event) {
        if (event.type() == QuestEventType.QUEST_COMPLETED) {
            publish(
                    event,
                    clock.instant(),
                    correlation(event) + ":reward"
            );
            return;
        }
        if (event.type() == QuestEventType.QUEST_REWARD_READY) {
            publish(event, event.occurredAt(), correlation(event));
        }
    }

    private void publish(
            QuestEvent source,
            Instant occurredAt,
            String correlationId
    ) {
        QuestRewardReadyFact.from(source, occurredAt, correlationId)
                .ifPresent(fact -> {
                    log.debug(
                            "Quest {} completed for player {}, publishing reward-ready fact",
                            fact.questCode(),
                            fact.playerId()
                    );
                    domainEventPublisher.publish(fact);
                });
    }

    private String correlation(QuestEvent event) {
        return event.correlationId() == null
                || event.correlationId().isBlank()
                ? event.key()
                : event.correlationId();
    }
}
