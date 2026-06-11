package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record CollectionLogged(
        Long playerId,
        Long collectionLogId,
        String category,
        int quantity,
        Instant occurredAt
) implements DomainEvent {
    public CollectionLogged {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static CollectionLogged of(
            Long playerId,
            Long collectionLogId,
            String category,
            int quantity
    ) {
        return new CollectionLogged(playerId, collectionLogId, category, quantity, Instant.now());
    }
}
