package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "PartyMember")
@Table(
        name = "party_members", indexes = {
        @Index(name = "idx_party_member_party", columnList = "party_id"),
        @Index(name = "idx_party_member_player", columnList = "player_id")
}
)
public class PartyMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private PartyMemberRole role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public static PartyMember createLeader(Party party, Long playerId) {
        PartyMember m = new PartyMember();
        m.party = party;
        m.playerId = playerId;
        m.role = PartyMemberRole.LEADER;
        m.joinedAt = LocalDateTime.now();
        return m;
    }

    public static PartyMember createMember(Party party, Long playerId) {
        PartyMember m = new PartyMember();
        m.party = party;
        m.playerId = playerId;
        m.role = PartyMemberRole.MEMBER;
        m.joinedAt = LocalDateTime.now();
        return m;
    }

    public void changeRole(PartyMemberRole role) {
        this.role = role;
    }
}
