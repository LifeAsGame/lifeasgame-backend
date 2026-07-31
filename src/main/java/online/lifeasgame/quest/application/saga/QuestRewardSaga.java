package online.lifeasgame.quest.application.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.quest.domain.event.QuestEvent;
import online.lifeasgame.quest.domain.event.QuestEventType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestRewardSaga {

    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onQuestEvent(QuestEvent event) {
        if (event.type() != QuestEventType.QUEST_COMPLETED) {
            return;
        }

        String correlation = (event.correlationId() == null || event.correlationId().isBlank())
                ? event.key() + ":reward"
                : event.correlationId() + ":reward";

        log.debug("Quest {} completed for player {}, scheduling reward pipeline", event.questCode(), event.playerId());

        domainEventPublisher.publish(
                QuestEvent.builder(QuestEventType.QUEST_REWARD_READY)
                        .questId(event.questId())
                        .questCode(event.questCode())
                        .playerId(event.playerId())
                        .attributes(event.attributes())
                        .occurredAt(clock.instant())
                        .correlationId(correlation)
                        .build()
        );
    }
}
