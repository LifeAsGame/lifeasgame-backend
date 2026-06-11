package online.lifeasgame.platform.events;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.event.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
