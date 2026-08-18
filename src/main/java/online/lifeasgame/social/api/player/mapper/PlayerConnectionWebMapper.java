package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.response.PlayerConnectionResponse;
import online.lifeasgame.social.application.result.ConnectionResult;

public final class PlayerConnectionWebMapper {

    private PlayerConnectionWebMapper() {
    }

    public static PlayerConnectionResponse.Page<PlayerConnectionResponse.Following> toFollowingPage(
            ConnectionResult.Page<ConnectionResult.Following> result
    ) {
        return new PlayerConnectionResponse.Page<>(
                result.contents().stream()
                        .map(item -> new PlayerConnectionResponse.Following(
                                item.followId(),
                                peer(item.peer()),
                                item.muted(),
                                item.blocked()
                        ))
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static PlayerConnectionResponse.Page<PlayerConnectionResponse.Follower> toFollowerPage(
            ConnectionResult.Page<ConnectionResult.Follower> result
    ) {
        return new PlayerConnectionResponse.Page<>(
                result.contents().stream()
                        .map(item -> new PlayerConnectionResponse.Follower(
                                peer(item.peer()),
                                item.followedBack(),
                                item.outboundFollowId()
                        ))
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private static PlayerConnectionResponse.Peer peer(ConnectionResult.Peer peer) {
        return new PlayerConnectionResponse.Peer(
                peer.playerId(),
                peer.name(),
                peer.job(),
                peer.level()
        );
    }
}
