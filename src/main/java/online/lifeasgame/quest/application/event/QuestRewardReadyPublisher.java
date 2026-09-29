package online.lifeasgame.quest.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.platform.outbox.application.OutboxEventDelivery;
import online.lifeasgame.quest.application.internal.event.QuestRewardReadyFact;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import online.lifeasgame.quest.infra.QuestRewardReadyPublicationStore;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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
    private final QuestRewardReadyPublicationStore publicationStore;

    @EventListener
    @Order(0) // Preserve publication before envelope notification failures.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOutboxEvent(OutboxEventDelivery delivery) {
        if (!(delivery.event() instanceof QuestEvent event)) {
            return;
        }
        if (event.type() == QuestEventType.QUEST_COMPLETED) {
            publish(
                    delivery.eventId(),
                    event,
                    clock.instant(),
                    correlation(event) + ":reward"
            );
            return;
        }
        if (event.type() == QuestEventType.QUEST_REWARD_READY) {
            publish(delivery.eventId(), event, event.occurredAt(), correlation(event));
        }
    }

    private void publish(
            String parentEventId,
            QuestEvent source,
            Instant occurredAt,
            String correlationId
    ) {
        QuestRewardReadyFact.from(source, occurredAt, correlationId)
                .ifPresent(fact -> {
                    if (!publicationStore.claim(parentEventId)) {
                        return;
                    }
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
