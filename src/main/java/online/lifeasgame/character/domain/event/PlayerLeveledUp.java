package online.lifeasgame.character.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record PlayerLeveledUp(
        Long playerId,
        int beforeLevel,
        int afterLevel,
        Instant occurredAt
) implements DomainEvent {

    public static PlayerLeveledUp of(Long playerId, int beforeLevel, int afterLevel) {
        return new PlayerLeveledUp(playerId, beforeLevel, afterLevel, Instant.now());
    }
}
