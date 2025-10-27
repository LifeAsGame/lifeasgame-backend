package online.lifeasgame.social.api.player.response;

import java.time.Instant;
import java.util.List;

public final class PlayerFollowResponse {

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
        public static Info of(
                Long id,
                Long playerId,
                Long targetPlayerId,
                String state,
                boolean muted,
                boolean blocked,
                Instant createdAt,
                Instant updatedAt
        ) {
            return new Info(id, playerId, targetPlayerId, state, muted, blocked, createdAt, updatedAt);
        }
    }

    public record Summary(Long id, Long playerId, Long targetPlayerId, String state, boolean muted, boolean blocked) {
        public static Summary of(
                Long id,
                Long playerId,
                Long targetPlayerId,
                String state,
                boolean muted,
                boolean blocked
        ) {
            return new Summary(id, playerId, targetPlayerId, state, muted, blocked);
        }
    }

    public record Page<T>(List<T> contents, int page, int size, long totalElements, int totalPages) {
        public static <T> Page<T> of(List<T> contents, int page, int size, long totalElements, int totalPages) {
            return new Page<>(contents, page, size, totalElements, totalPages);
        }
    }
}
