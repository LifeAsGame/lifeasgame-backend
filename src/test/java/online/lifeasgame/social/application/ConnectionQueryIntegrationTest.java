package online.lifeasgame.social.application;

import jakarta.persistence.EntityManager;
import online.lifeasgame.character.domain.GenderType;
import online.lifeasgame.character.domain.Name;
import online.lifeasgame.character.domain.Player;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.ConnectionResult;
import online.lifeasgame.social.application.result.FollowResult;
import online.lifeasgame.social.domain.error.SocialError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Current Player Connections query")
class ConnectionQueryIntegrationTest {

    @Autowired
    private ConnectionQueryService connectionQueryService;

    @Autowired
    private FollowService followService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentPlayerAccessor currentPlayerAccessor;

    @Nested
    @DisplayName("팔로잉 연결을 조회할 때")
    class Followings {

        @Test
        @DisplayName("active outbound peer와 현재 Player의 mute/block 상태만 반환한다")
        void returnsEnrichedOutboundState() {
            Player current = currentPlayer();
            Player activePeer = player(28402L, "Active Peer");
            Player stoppedPeer = player(28403L, "Stopped Peer");
            FollowResult.Info active = follow(current.getId(), activePeer.getId());
            FollowResult.Info stopped = follow(current.getId(), stoppedPeer.getId());
            followService.mute(current.getId(), active.id());
            followService.block(current.getId(), active.id());
            followService.unfollow(current.getId(), stopped.id());

            ConnectionResult.Page<ConnectionResult.Following> result =
                    connectionQueryService.followings(0, 20);

            assertThat(result.contents()).containsExactly(new ConnectionResult.Following(
                    active.id(),
                    new ConnectionResult.Peer(activePeer.getId(), "Active Peer", null, 1),
                    true,
                    true
            ));
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("팔로워 연결을 조회할 때")
    class Followers {

        @Test
        @DisplayName("active inbound peer를 반환하고 active reverse만 action state로 연결한다")
        void resolvesReverseFollowInBatch() {
            Player current = currentPlayer();
            Player followedBackPeer = player(28412L, "Followed Back");
            Player stoppedReversePeer = player(28413L, "Stopped Reverse");
            Player stoppedFollower = player(28414L, "Stopped Follower");
            FollowResult.Info inboundWithReverse = follow(followedBackPeer.getId(), current.getId());
            follow(stoppedReversePeer.getId(), current.getId());
            FollowResult.Info inactiveInbound = follow(stoppedFollower.getId(), current.getId());
            FollowResult.Info activeReverse = follow(current.getId(), followedBackPeer.getId());
            FollowResult.Info stoppedReverse = follow(current.getId(), stoppedReversePeer.getId());
            followService.mute(followedBackPeer.getId(), inboundWithReverse.id());
            followService.block(followedBackPeer.getId(), inboundWithReverse.id());
            followService.unfollow(current.getId(), stoppedReverse.id());
            followService.unfollow(stoppedFollower.getId(), inactiveInbound.id());

            ConnectionResult.Page<ConnectionResult.Follower> result =
                    connectionQueryService.followers(0, 20);

            assertThat(result.contents()).extracting(item -> item.peer().playerId())
                    .containsExactly(stoppedReversePeer.getId(), followedBackPeer.getId());
            assertThat(result.contents()).anySatisfy(item -> {
                assertThat(item.peer()).isEqualTo(new ConnectionResult.Peer(
                        followedBackPeer.getId(),
                        "Followed Back",
                        null,
                        1
                ));
                assertThat(item.followedBack()).isTrue();
                assertThat(item.outboundFollowId()).isEqualTo(activeReverse.id());
            });
            assertThat(result.contents()).anySatisfy(item -> {
                assertThat(item.peer()).isEqualTo(new ConnectionResult.Peer(
                        stoppedReversePeer.getId(),
                        "Stopped Reverse",
                        null,
                        1
                ));
                assertThat(item.followedBack()).isFalse();
                assertThat(item.outboundFollowId()).isNull();
            });
            assertThat(result.totalElements()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Follow peer Player가 없을 때")
    class MissingPeer {

        @Test
        @DisplayName("이름을 만들지 않고 controlled connection error로 거부한다")
        void rejectsMissingPlayerSummary() {
            Player current = currentPlayer();
            follow(current.getId(), 999_999L);

            assertThatThrownBy(() -> connectionQueryService.followings(0, 20))
                    .isInstanceOfSatisfying(DomainException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(
                                    SocialError.CONNECTION_PEER_NOT_FOUND
                            )
                    );
        }
    }

    private Player currentPlayer() {
        Player current = player(28401L, "Current Player");
        given(currentPlayerAccessor.currentPlayerIdOrThrow()).willReturn(current.getId());
        return current;
    }

    private Player player(Long userId, String name) {
        Player player = Player.linkStart(userId, Name.of(name), GenderType.MALE);
        entityManager.persist(player);
        entityManager.flush();
        return player;
    }

    private FollowResult.Info follow(Long playerId, Long targetPlayerId) {
        return followService.follow(playerId, new FollowCommand.Create(targetPlayerId));
    }
}
