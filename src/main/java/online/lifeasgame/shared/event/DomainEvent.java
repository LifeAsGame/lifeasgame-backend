package online.lifeasgame.shared.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
