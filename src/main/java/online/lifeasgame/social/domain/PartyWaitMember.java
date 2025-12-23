package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "PartyWaitMember")
@Table(
        name = "party_wait_members", indexes = {
        @Index(name = "idx_wait_party", columnList = "party_id"),
        @Index(name = "idx_wait_player", columnList = "player_id")
}
)
public class PartyWaitMember extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_wait_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private PartyWaitType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PartyWaitStatus status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public PartyWaitMember(
            Party party,
            Long playerId,
            PartyWaitType type,
            PartyWaitStatus status,
            String message,
            LocalDateTime requestedAt,
            LocalDateTime expiresAt
    ) {
        this.party = party;
        this.playerId = playerId;
        this.type = type;
        this.status = status;
        this.message = message;
        this.requestedAt = requestedAt;
        this.expiresAt = expiresAt;
    }

    public static PartyWaitMember joinRequest(Party party, Long playerId, String message) {
        return new PartyWaitMember(
                party,
                playerId,
                PartyWaitType.JOIN_REQUEST,
                PartyWaitStatus.PENDING,
                message,
                LocalDateTime.now(),
                null
        );
    }

    public static PartyWaitMember invitation(
            Party party,
            Long invitee,
            String message,
            LocalDateTime expiresAt
    ) {
        return new PartyWaitMember(
                party,
                invitee,
                PartyWaitType.INVITATION,
                PartyWaitStatus.PENDING,
                message,
                LocalDateTime.now(),
                expiresAt
        );
    }

    public void approve() {
        this.status = PartyWaitStatus.APPROVED;
    }

    public void reject() {
        this.status = PartyWaitStatus.REJECTED;
    }

    public void cancel() {
        this.status = PartyWaitStatus.CANCELLED;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}
