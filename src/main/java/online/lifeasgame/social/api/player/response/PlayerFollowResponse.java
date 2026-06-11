package online.lifeasgame.social.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerFollowResponse {

    private PlayerFollowResponse() {
    }

    public record Info(
            Long id,
            Long playerId,
            Long targetPlayerId,
            String state,
            boolean muted,
            boolean blocked,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record Summary(
            Long id,
            Long playerId,
            Long targetPlayerId,
            String state,
            boolean muted,
            boolean blocked
    ) {
    }

    public record Relationship(
            Long followId,
            Long playerId,
            Long targetPlayerId,
            String state,
            boolean muted,
            boolean blocked
    ) {}

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
