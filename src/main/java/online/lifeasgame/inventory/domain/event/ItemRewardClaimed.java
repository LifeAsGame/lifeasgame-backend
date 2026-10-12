package online.lifeasgame.inventory.domain.event;

import java.time.Instant;
import online.lifeasgame.core.event.DomainEvent;

/** Committed transfer from one stable mailbox entry into Inventory. */
public record ItemRewardClaimed(
        Long playerId,
        Long mailboxEntryId,
        Long itemId,
        int quantity,
        Instant occurredAt
) implements DomainEvent {}
