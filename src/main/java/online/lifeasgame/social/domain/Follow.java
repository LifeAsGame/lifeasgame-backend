package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AggregateRoot
@Entity(name = "Follow")
@Table(
        name = "follows", uniqueConstraints = @UniqueConstraint(
        name = "uk_follower_followee", columnNames = {"player_id", "target_player_id"}
), indexes = {
        @Index(name = "idx_follow_player", columnList = "player_id"),
        @Index(name = "idx_follow_target_player", columnList = "target_player_id")
}
)
public class Follow extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "follow_id")
    private Long id;

    /**
     * 소유자(간접참조): 팔로우를 거는 주체
     */
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    /**
     * 대상(간접참조): 팔로우를 당하는 주체
     */
    @Column(name = "target_player_id", nullable = false)
    private Long targetPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private FollowState state;

    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    public static Follow create(Long playerId, Long targetPlayerId) {
        Guard.notNull(playerId, "playerId");
        Guard.notNull(targetPlayerId, "targetPlayerId");
        Guard.check(!playerId.equals(targetPlayerId), "cannot follow self");

        Follow f = new Follow();
        f.playerId = playerId;
        f.targetPlayerId = targetPlayerId;
        f.state = FollowState.FOLLOWING;
        f.muted = false;
        f.blocked = false;
        return f;
    }

    // ===== 도메인 행위 =====
    public void unfollow() {
        Guard.checkState(this.state == FollowState.FOLLOWING, "not following");
        this.state = FollowState.STOPPED;
        this.muted = false;
    }

    public void mute() {
        Guard.checkState(this.state == FollowState.FOLLOWING, "mute only when following");
        this.muted = true;
    }

    public void unmute() {
        this.muted = false;
    }

    public void block() {
        this.blocked = true;
        // Block 시에도 레코드는 유지하되, 상호작용은 상위 계층 정책으로 제한
    }

    public void unblock() {
        this.blocked = false;
    }
}
