package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalDomainEventDispatcher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void dispatch(String eventId, DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
        applicationEventPublisher.publishEvent(
                new OutboxEventDelivery(eventId, event)
        );
    }
}
