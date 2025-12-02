package online.lifeasgame.economy.infra.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import online.lifeasgame.economy.domain.event.EconomyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "economyEventKafkaTemplate")
public class KafkaEconomyEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEconomyEventPublisher.class);

    private final KafkaTemplate<String, EconomyEvent> economyEventKafkaTemplate;
    private final EconomyEventProperties properties;

    public void publish(EconomyEvent event) {
        economyEventKafkaTemplate.send(properties.getTopic(), event.key(), event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to forward economy event {}", event.type(), ex);
                    } else if (log.isTraceEnabled()) {
                        log.trace("Economy event {} forwarded to Kafka topic {}", event.type(), properties.getTopic());
                    }
                });
    }
}
