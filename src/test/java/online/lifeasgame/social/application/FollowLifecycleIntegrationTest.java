package online.lifeasgame.social.application;

import jakarta.persistence.EntityManager;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.FollowCommand;
import online.lifeasgame.social.application.result.FollowResult;
import online.lifeasgame.social.domain.Follow;
import online.lifeasgame.social.domain.FollowState;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.FollowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Follow lifecycle와 friendship contract")
class FollowLifecycleIntegrationTest {

    private static final Long PLAYER_ID = 282L;
    private static final Long FRIEND_ID = 283L;

    @Autowired
    private FollowService followService;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private FriendshipVerifier friendshipVerifier;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("같은 pair를 다시 follow할 때")
    class Refollow {

        @Test
        @DisplayName("기존 row를 재사용하고 STOPPED 상태와 mute만 복구한다")
        void reusesExistingRow() {
            FollowResult.Info first = follow(PLAYER_ID, FRIEND_ID);
            FollowResult.Info alreadyFollowing = follow(PLAYER_ID, FRIEND_ID);
            followService.block(PLAYER_ID, first.id());
            followService.mute(PLAYER_ID, first.id());
            followService.unfollow(PLAYER_ID, first.id());

            FollowResult.Info followedAgain = follow(PLAYER_ID, FRIEND_ID);
            entityManager.flush();
            Follow persisted = followRepository.findByPlayerIdAndTargetPlayerId(
                    PLAYER_ID,
                    FRIEND_ID
            ).orElseThrow();

            assertThat(alreadyFollowing.id()).isEqualTo(first.id());
            assertThat(followedAgain.id()).isEqualTo(first.id());
            assertThat(pairRowCount(PLAYER_ID, FRIEND_ID)).isEqualTo(1);
            assertThat(persisted.getState()).isEqualTo(FollowState.FOLLOWING);
            assertThat(persisted.isMuted()).isFalse();
            assertThat(persisted.isBlocked()).isTrue();
        }
    }

    @Nested
    @DisplayName("follow 목록과 집계를 조회할 때")
    class ActiveQueries {

        @Test
        @DisplayName("양방향 모두 FOLLOWING만 반환하고 follower는 inbound로 집계한다")
        void returnsOnlyActiveRelationships() {
            FollowResult.Info activeFollowing = follow(PLAYER_ID, FRIEND_ID);
            FollowResult.Info stoppedFollowing = follow(PLAYER_ID, FRIEND_ID + 1);
            FollowResult.Info activeFollower = follow(FRIEND_ID + 2, PLAYER_ID);
            FollowResult.Info stoppedFollower = follow(FRIEND_ID + 3, PLAYER_ID);
            followService.unfollow(PLAYER_ID, stoppedFollowing.id());
            followService.unfollow(FRIEND_ID + 3, stoppedFollower.id());

            FollowResult.Page<FollowResult.Summary> followings =
                    followService.listFollowings(PLAYER_ID, 0, 20);
            FollowResult.Page<FollowResult.Summary> followers =
                    followService.listFollowers(PLAYER_ID, 0, 20);

            assertThat(followings.contents()).extracting(FollowResult.Summary::id)
                    .containsExactly(activeFollowing.id());
            assertThat(followings.totalElements()).isEqualTo(1);
            assertThat(followers.contents()).extracting(FollowResult.Summary::id)
                    .containsExactly(activeFollower.id());
            assertThat(followers.totalElements()).isEqualTo(1);
            assertThat(followService.recentFollowings(PLAYER_ID, 20))
                    .extracting(FollowResult.Summary::id)
                    .containsExactly(activeFollowing.id());
            assertThat(followService.recentFollowers(PLAYER_ID, 20))
                    .extracting(FollowResult.Summary::id)
                    .containsExactly(activeFollower.id());
        }
    }

    @Nested
    @DisplayName("friendship을 검증할 때")
    class Friendship {

        @Test
        @DisplayName("양방향 active follow이면 허용한다")
        void allowsMutualActiveFollow() {
            follow(PLAYER_ID, FRIEND_ID);
            follow(FRIEND_ID, PLAYER_ID);

            assertThatCode(() -> friendshipVerifier.verify(PLAYER_ID, FRIEND_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("반대 방향이 없거나 STOPPED이면 NOT_FRIEND로 거부한다")
        void rejectsMissingOrStoppedDirection() {
            follow(PLAYER_ID, FRIEND_ID);

            assertNotFriend();

            FollowResult.Info reverse = follow(FRIEND_ID, PLAYER_ID);
            followService.unfollow(FRIEND_ID, reverse.id());

            assertNotFriend();
        }
    }

    private FollowResult.Info follow(Long playerId, Long targetPlayerId) {
        return followService.follow(playerId, new FollowCommand.Create(targetPlayerId));
    }

    private long pairRowCount(Long playerId, Long targetPlayerId) {
        return entityManager.createQuery("""
                SELECT COUNT(f)
                FROM Follow f
                WHERE f.playerId = :playerId
                  AND f.targetPlayerId = :targetPlayerId
                """, Long.class)
                .setParameter("playerId", playerId)
                .setParameter("targetPlayerId", targetPlayerId)
                .getSingleResult();
    }

    private void assertNotFriend() {
        assertThatThrownBy(() -> friendshipVerifier.verify(PLAYER_ID, FRIEND_ID))
                .isInstanceOfSatisfying(DomainException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(SocialError.NOT_FRIEND)
                );
    }
}
