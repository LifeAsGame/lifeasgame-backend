package online.lifeasgame.platform.outbox.application;

import online.lifeasgame.core.event.DomainEvent;

public record OutboxEventDelivery(
        String eventId,
        DomainEvent event
) {
}
