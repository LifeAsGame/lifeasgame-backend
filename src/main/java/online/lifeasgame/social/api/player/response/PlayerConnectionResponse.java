package online.lifeasgame.social.api.player.response;

import java.util.List;

public final class PlayerConnectionResponse {

    private PlayerConnectionResponse() {
    }

    public record Page<T>(
            List<T> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
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
