package online.lifeasgame.social.api.admin.response;

import java.util.List;

public final class AdminFollowResponse {
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
