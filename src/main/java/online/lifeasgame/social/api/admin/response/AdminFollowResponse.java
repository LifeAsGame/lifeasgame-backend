package online.lifeasgame.social.api.admin.response;

import java.util.List;

public final class AdminFollowResponse {

    private AdminFollowResponse() {}

    public record Summary(
            Long id,
            Long playerId,
            Long targetPlayerId,
            String state,
            boolean muted,
            boolean blocked
    ) {
    }

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }
}
