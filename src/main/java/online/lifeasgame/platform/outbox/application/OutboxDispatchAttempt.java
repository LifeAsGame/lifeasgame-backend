package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.platform.outbox.application.codec.OutboxEventCodecRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxDispatchAttempt {

    private final OutboxEventCodecRegistry codecRegistry;
    private final LocalDomainEventDispatcher dispatcher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(OutboxClaim claim) {
        DomainEvent event = codecRegistry.decode(
                claim.eventType(),
                claim.payload()
        );
        dispatcher.dispatch(claim.eventId(), event);
    }
}
