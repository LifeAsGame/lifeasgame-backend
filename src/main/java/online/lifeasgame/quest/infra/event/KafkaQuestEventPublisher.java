package online.lifeasgame.quest.infra.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.quest.domain.event.QuestEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "questEventKafkaTemplate")
class KafkaQuestEventPublisher {

    private final KafkaTemplate<String, QuestEvent> questEventKafkaTemplate;
    private final QuestEventProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(QuestEvent event) {
        questEventKafkaTemplate.send(properties.getTopic(), event.key(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish quest event {}", event, ex);
                        return;
                    }
                    log.trace("Quest event {} forwarded to Kafka topic {}", event.type(), properties.getTopic());
                });
    }
}
