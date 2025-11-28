package online.lifeasgame.inventory.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record InventoryItemAdded(
        Long playerId,
        Long itemId,
        String rarity,
        boolean stackable,
        boolean bound,
        int quantity,
        Instant occurredAt
) implements DomainEvent {
    public InventoryItemAdded {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static InventoryItemAdded of(
            Long playerId,
            Long itemId,
            String rarity,
            boolean stackable,
            boolean bound,
            int quantity
    ) {
        return new InventoryItemAdded(playerId, itemId, rarity, stackable, bound, quantity, Instant.now());
    }
}
