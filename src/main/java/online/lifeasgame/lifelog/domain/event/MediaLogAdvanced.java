package online.lifeasgame.lifelog.domain.event;

import online.lifeasgame.core.event.DomainEvent;

import java.time.Instant;

public record MediaLogAdvanced(
        Long playerId,
        Long mediaLogId,
        int advancedBy,
        int currentStep,
        int totalEpisodes,
        Instant occurredAt
) implements DomainEvent {
    public MediaLogAdvanced {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public static MediaLogAdvanced of(
            Long playerId,
            Long mediaLogId,
            int advancedBy,
            int currentStep,
            int totalEpisodes
    ) {
        return new MediaLogAdvanced(playerId, mediaLogId, advancedBy, currentStep, totalEpisodes, Instant.now());
    }
}
