package online.lifeasgame.quest.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.quest.domain.event.QuestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lifeasgame.quest.events", name = "enabled", havingValue = "true")
class RedisQuestEventPublisher {

    private final RedisTemplate<String, QuestEvent> questEventRedisTemplate;
    private final QuestEventProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(QuestEvent event) {
        try {
            questEventRedisTemplate.convertAndSend(properties.getChannel(), event);
            log.trace("Quest event {} published to redis channel {}", event.type(), properties.getChannel());
        } catch (Exception e) {
            log.error("Failed to publish quest event {}", event, e);
        }
    }
}
