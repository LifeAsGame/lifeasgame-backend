package online.lifeasgame.economy.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lifeasgame.economy.events", name = "enabled", havingValue = "true")
public class RedisEconomyEventPublisher {

    private final RedisTemplate<String, EconomyEvent> economyEventRedisTemplate;
    private final EconomyEventProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EconomyEvent event) {
        try {
            economyEventRedisTemplate.convertAndSend(properties.getChannel(), event);
            log.trace("Economy event {} published to redis channel {}",
                    event.type(), properties.getChannel());
        } catch (Exception e) {
            log.error("Failed to publish economy event {}", event.type(), e);
        }
    }
}
