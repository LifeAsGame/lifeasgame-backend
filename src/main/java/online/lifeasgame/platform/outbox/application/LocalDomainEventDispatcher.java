package online.lifeasgame.platform.outbox.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalDomainEventDispatcher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void dispatch(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
