package online.lifeasgame.character.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record PlayerRegistered(
        Long playerId,
        Instant occurredAt
) implements DomainEvent {
    public static PlayerRegistered of(Long playerId) {
        return new PlayerRegistered(playerId, Instant.now());
    }
}
