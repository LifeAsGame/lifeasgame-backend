package online.lifeasgame.platform.outbox.application.codec;

import online.lifeasgame.core.event.DomainEvent;

public interface OutboxEventCodec<T extends DomainEvent> {

    String alias();

    Class<T> eventType();

    String encode(T event);

    T decode(String payload);
}
