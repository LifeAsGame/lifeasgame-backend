package online.lifeasgame.core.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
