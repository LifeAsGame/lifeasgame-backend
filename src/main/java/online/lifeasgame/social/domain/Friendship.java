package online.lifeasgame.social.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

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
    @AttributeOverrides({
            @AttributeOverride(name = "low", column = @Column(name = "low_id", nullable = false)),
            @AttributeOverride(name = "high", column = @Column(name = "high_id", nullable = false))
    })
    private PairKey pair;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FriendshipStatus status;

    @Column(name = "requested_by")
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
        Guard.check(involves(byUser), "not a participant");
        Guard.checkState(status == FriendshipStatus.PENDING, "not pending");
        Guard.check(!requestedBy.equals(byUser), "request cannot accept");
        status = FriendshipStatus.ACCEPTED;
        requestedBy = null;
    }

    public void block(Long byUser) {
        Guard.check(involves(byUser), "not a participant");
        status = FriendshipStatus.BLOCKED;
        blockedBy = byUser;
    }

    public void unblock(Long byUser) {
        Guard.check(status == FriendshipStatus.PENDING, "not pending");
        Guard.check(byUser.equals(blockedBy), "block cannot unblock");
        status = (requestedBy != null) ? FriendshipStatus.PENDING : FriendshipStatus.ACCEPTED;
        blockedBy = null;
    }
}
