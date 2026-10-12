package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "role_party_invitations", uniqueConstraints = @UniqueConstraint(name = "uq_role_party_invitee", columnNames = {"role_party_id", "invitee_player_id"}))
public class RolePartyInvitation extends AbstractTime {
    public enum Status { PENDING, ACCEPTED, DECLINED, CANCELED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_party_id", nullable = false, updatable = false)
    private RoleParty party;
    @Column(name = "inviter_player_id", nullable = false)
    private Long inviterPlayerId;
    @Column(name = "invitee_player_id", nullable = false, updatable = false)
    private Long inviteePlayerId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status;

    RolePartyInvitation(RoleParty party, Long inviter, Long invitee, Instant expiresAt) {
        this.party = party;
        this.inviterPlayerId = inviter;
        this.inviteePlayerId = invitee;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    void resend(Long inviter, Instant now) {
        if (status == Status.PENDING && expiresAt.isAfter(now)) return;
        inviterPlayerId = inviter;
        expiresAt = now.plusSeconds(7 * 24 * 3600);
        status = Status.PENDING;
    }

    void requirePending(Instant now) {
        if (status == Status.PENDING && !expiresAt.isAfter(now)) {
            throw new DomainException(SocialError.ROLE_PARTY_INVITATION_EXPIRED);
        }
        if (status != Status.PENDING) throw new DomainException(SocialError.ROLE_PARTY_INVALID_STATE);
    }

    void accept() { status = Status.ACCEPTED; }
    void decline() { status = Status.DECLINED; }
    void cancel() {
        if (status != Status.PENDING) throw new DomainException(SocialError.ROLE_PARTY_INVALID_STATE);
        status = Status.CANCELED;
    }
}
