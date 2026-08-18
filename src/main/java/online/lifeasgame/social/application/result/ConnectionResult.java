package online.lifeasgame.social.application.result;

import java.util.List;

public final class ConnectionResult {

    private ConnectionResult() {
    }

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static <T> Page<T> of(
                List<T> contents,
                int page,
                int size,
                long totalElements
        ) {
            return new Page<>(
                    contents,
                    page,
                    size,
                    totalElements,
                    (int) Math.ceil(totalElements / (double) Math.max(size, 1))
            );
        }
    }

    public record Peer(
            Long playerId,
            String name,
            String job,
            int level
    ) {
    }

    public record Following(
            Long followId,
            Peer peer,
            boolean muted,
            boolean blocked
    ) {
    }

    public record Follower(
            Peer peer,
            boolean followedBack,
            Long outboundFollowId
    ) {
    }
}
