package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.annotation.AggregateRoot;
import online.lifeasgame.shared.entity.AbstractTime;

@Entity
@AggregateRoot
@Table(
        name = "friendships",
        uniqueConstraints = @UniqueConstraint(name = "uq_friend_pair", columnNames = {"low_id", "high_id"}),
        indexes = {@Index(name = "idx_friend_status", columnList = "status")}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private PairKey pair;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FriendshipStatus status;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "blocked_by")
    private Long blockedBy;

    @Version
    private Long version;

    private Friendship(PairKey pair, FriendshipStatus status, Long requestedBy) {
        this.pair = pair;
        this.status = status;
        this.requestedBy = requestedBy;
    }

    public static Friendship request(Long me, Long other) {
        return new Friendship(PairKey.of(me, other), FriendshipStatus.PENDING, me);
    }

    public boolean involves(Long userId) {
        return pair.low().equals(userId) || pair.high().equals(userId);
    }

    public void accept(Long byUser) {
        if (!involves(byUser)) {
            throw new IllegalStateException("not a participant");
        }
        if (status != FriendshipStatus.PENDING) {
            throw new IllegalStateException("not pending");
        }
        if (requestedBy.equals(byUser)) {
            throw new IllegalStateException("requester cannot accept");
        }
        status = FriendshipStatus.ACCEPTED;
        requestedBy = null; // requester no longer relevant
    }

    public void block(Long byUser) {
        if (!involves(byUser)) {
            throw new IllegalStateException();
        }
        status = FriendshipStatus.BLOCKED;
        blockedBy = byUser;
    }

    public void unblock(Long byUser) {
        if (status != FriendshipStatus.BLOCKED || !byUser.equals(blockedBy)) {
            throw new IllegalStateException();
        }
        status = FriendshipStatus.ACCEPTED;
        blockedBy = null;
    }
}
