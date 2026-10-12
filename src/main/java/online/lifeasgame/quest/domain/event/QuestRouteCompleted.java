package online.lifeasgame.quest.domain.event;

import java.time.Instant;
import online.lifeasgame.core.event.DomainEvent;

/** Fact emitted only by the final explicit route advance. */
public record QuestRouteCompleted(
        Long playerId,
        Long playerRouteId,
        String routeCode,
        Instant occurredAt
) implements DomainEvent {}
