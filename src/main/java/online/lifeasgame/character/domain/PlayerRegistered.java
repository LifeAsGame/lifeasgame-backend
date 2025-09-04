package online.lifeasgame.character.domain;

import java.time.Instant;
import online.lifeasgame.core.event.DomainEvent;

public record PlayerRegistered(Instant occurredAt) implements DomainEvent {
    public static PlayerRegistered of() {
        return new PlayerRegistered(Instant.now());
    }
}
