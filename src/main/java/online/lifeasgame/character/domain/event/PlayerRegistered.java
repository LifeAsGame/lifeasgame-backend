package online.lifeasgame.character.domain.event;

import java.time.Instant;
import online.lifeasgame.core.event.DomainEvent;

public record PlayerRegistered(Long playerId, Instant occurredAt) implements DomainEvent {
    public static PlayerRegistered of(Long playerId) {
        return new PlayerRegistered(playerId, Instant.now());
    }
}
