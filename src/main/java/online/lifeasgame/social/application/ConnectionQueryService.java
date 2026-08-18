package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi;
import online.lifeasgame.character.application.internal.PlayerConnectionReadApi.PlayerSummary;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.result.ConnectionResult;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectionQueryService {

    private final CurrentPlayerAccessor currentPlayerAccessor;
    private final FollowReader followReader;
    private final PlayerConnectionReadApi playerConnectionReadApi;

    public ConnectionResult.Page<ConnectionResult.Following> followings(
            int page,
            int size
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<Follow> followings = followReader.getFollowingsByPlayerId(
                playerId,
                page,
                size
        );
        Map<Long, PlayerSummary> peers = playerConnectionReadApi.findAllByPlayerIds(followings.stream()
                .map(Follow::getTargetPlayerId)
                .collect(Collectors.toUnmodifiableSet()));
        List<ConnectionResult.Following> contents = followings.stream()
                .map(follow -> new ConnectionResult.Following(
                        follow.getId(),
                        peer(requirePeer(peers, follow.getTargetPlayerId())),
                        follow.isMuted(),
                        follow.isBlocked()
                ))
                .toList();
        return ConnectionResult.Page.of(
                contents,
                page,
                size,
                followReader.countFollowings(playerId)
        );
    }

    public ConnectionResult.Page<ConnectionResult.Follower> followers(
            int page,
            int size
    ) {
        Long playerId = currentPlayerAccessor.currentPlayerIdOrThrow();
        List<Follow> followers = followReader.getFollowersByPlayerId(
                playerId,
                page,
                size
        );
        Set<Long> peerIds = followers.stream()
                .map(Follow::getPlayerId)
                .collect(Collectors.toUnmodifiableSet());
        Map<Long, PlayerSummary> peers = playerConnectionReadApi.findAllByPlayerIds(peerIds);
        Map<Long, Follow> outboundByPeerId = followReader
                .findActiveFollowings(playerId, peerIds)
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        Follow::getTargetPlayerId,
                        Function.identity()
                ));
        List<ConnectionResult.Follower> contents = followers.stream()
                .map(follow -> {
                    Long peerId = follow.getPlayerId();
                    Follow outbound = outboundByPeerId.get(peerId);
                    return new ConnectionResult.Follower(
                            peer(requirePeer(peers, peerId)),
                            outbound != null,
                            outbound == null ? null : outbound.getId()
                    );
                })
                .toList();
        return ConnectionResult.Page.of(
                contents,
                page,
                size,
                followReader.countFollowers(playerId)
        );
    }

    private static PlayerSummary requirePeer(
            Map<Long, PlayerSummary> peers,
            Long playerId
    ) {
        PlayerSummary peer = peers.get(playerId);
        if (peer == null) {
            throw new DomainException(SocialError.CONNECTION_PEER_NOT_FOUND);
        }
        return peer;
    }

    private static ConnectionResult.Peer peer(PlayerSummary peer) {
        return new ConnectionResult.Peer(
                peer.playerId(),
                peer.name(),
                peer.job(),
                peer.level()
        );
    }
}
