package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventEnvelope;
import online.lifeasgame.platform.outbox.domain.OutboxEvent;
import online.lifeasgame.platform.outbox.domain.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class TransactionalOutboxDomainEventPublisher
        implements DomainEventPublisher {

    private final OutboxEventCodecRegistry codecRegistry;
    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    @Override
    public void publish(DomainEvent event) {
        requireTransaction();
        OutboxEventEnvelope envelope = codecRegistry.encode(event);
        outboxEventRepository.save(
                OutboxEvent.pending(
                        UUID.randomUUID().toString(),
                        envelope.eventType(),
                        envelope.payload(),
                        envelope.occurredAt(),
                        clock.instant()
                )
        );
    }

    @Override
    public void publishAll(Collection<? extends DomainEvent> events) {
        requireTransaction();
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(this::publish);
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalTransactionStateException(
                    "Domain events must be appended within an active transaction"
            );
        }
    }
}
