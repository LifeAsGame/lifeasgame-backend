package online.lifeasgame.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.core.guard.Guard;

@Getter
@Entity
@Table(
        name = "party_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_party_member", columnNames = {"party_id", "player_id"}),
        indexes = @Index(name = "idx_party_member_player", columnList = "player_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PartyRole role = PartyRole.MEMBER;

    @Version
    private Long version;

    private PartyMember(Party party, Long playerId, PartyRole role) {
        Guard.notNull(party, "party");
        Guard.notNull(playerId, "playerId");
        Guard.minValue(playerId, 1, "playerId");
        this.party = party;
        this.playerId = playerId;
        this.role = role == null ? PartyRole.MEMBER : role;
    }
}
