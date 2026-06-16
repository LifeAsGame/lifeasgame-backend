package online.lifeasgame.economy.application.event;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.infra.event.RedisEconomyEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lifeasgame.economy.events", name = "enabled", havingValue = "true")
public class EconomyEventBridge {

    private static final Logger log = LoggerFactory.getLogger(EconomyEventBridge.class);

    private final RedisEconomyEventPublisher redisPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEconomyEvent(EconomyEvent event) {
        log.debug("Forwarding economy event {}", event.type());
        redisPublisher.publish(event);
    }
}
