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

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "target_player_id", nullable = false)
    private Long targetPlayerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private FollowState state;

    @Column(name = "muted", nullable = false)
    private boolean muted;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    private Follow(
            Long playerId,
            Long targetPlayerId,
            FollowState state,
            boolean muted,
            boolean blocked
    ) {
        this.playerId = playerId;
        this.targetPlayerId = targetPlayerId;
        this.state = state;
        this.muted = muted;
        this.blocked = blocked;
    }

    public static Follow create(Long playerId, Long targetPlayerId) {
        Guard.notNull(playerId, "playerId");
        Guard.notNull(targetPlayerId, "targetPlayerId");
        Guard.check(!playerId.equals(targetPlayerId), "cannot follow self");

        return new Follow(
                playerId,
                targetPlayerId,
                FollowState.FOLLOWING,
                false,
                false
        );
    }

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
    }

    public void unblock() {
        this.blocked = false;
    }
}
