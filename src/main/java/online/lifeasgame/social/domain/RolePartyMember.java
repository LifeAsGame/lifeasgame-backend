package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "role_party_members", uniqueConstraints = @UniqueConstraint(name = "uq_role_party_member", columnNames = {"role_party_id", "player_id"}))
public class RolePartyMember extends AbstractTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_party_id", nullable = false, updatable = false)
    private RoleParty party;
    @Column(name = "player_id", nullable = false, updatable = false)
    private Long playerId;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;
    @Column(name = "left_at")
    private Instant leftAt;

    RolePartyMember(RoleParty party, Long playerId, Instant now) {
        this.party = party;
        this.playerId = playerId;
        this.joinedAt = now;
    }

    public boolean isActive() { return leftAt == null; }
    void leave() { leftAt = Instant.now(); }
    void rejoin(Instant now) { leftAt = null; joinedAt = now; }
}
