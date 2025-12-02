package online.lifeasgame.economy.application.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.infra.event.KafkaEconomyEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaEconomyEventPublisher.class)
public class EconomyEventBridge {

    private static final Logger log = LoggerFactory.getLogger(EconomyEventBridge.class);

    private final KafkaEconomyEventPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEconomyEvent(EconomyEvent event) {
        log.debug("Forwarding economy event {}", event.type());
        kafkaPublisher.publish(event);
    }
}
