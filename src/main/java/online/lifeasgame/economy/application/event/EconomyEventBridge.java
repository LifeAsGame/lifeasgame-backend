package online.lifeasgame.economy.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import online.lifeasgame.economy.infra.event.KafkaEconomyEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaEconomyEventPublisher.class)
public class EconomyEventBridge {

    private final KafkaEconomyEventPublisher kafkaPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEconomyEvent(EconomyEvent event) {
        log.debug("Forwarding economy event {}", event.type());
        kafkaPublisher.publish(event);
    }
}
