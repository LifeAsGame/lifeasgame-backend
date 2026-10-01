package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@AggregateRoot
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "role_parties", indexes = @Index(name = "idx_role_party_creator_role", columnList = "creator_player_id,role_id,id"))
public class RoleParty extends AbstractTime {
    public enum Status { ACTIVE, DISBANDED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "role_id", nullable = false, updatable = false)
    private Long roleId;
    @Column(name = "creator_player_id", nullable = false, updatable = false)
    private Long creatorPlayerId;
    @Column(name = "leader_player_id", nullable = false)
    private Long leaderPlayerId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "max_members", nullable = false)
    private int maxMembers;
    @Version @Column(nullable = false)
    private long version;
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<RolePartyMember> members = new ArrayList<>();
    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL)
    private List<RolePartyInvitation> invitations = new ArrayList<>();

    public static RoleParty create(Long roleId, Long creatorPlayerId, String name, String description, int maxMembers) {
        RoleParty party = new RoleParty();
        party.roleId = roleId;
        party.creatorPlayerId = creatorPlayerId;
        party.leaderPlayerId = creatorPlayerId;
        party.status = Status.ACTIVE;
        party.updateFields(name, description, maxMembers);
        party.members.add(new RolePartyMember(party, creatorPlayerId, Instant.now()));
        return party;
    }

    public void update(Long actor, String name, String description, int maxMembers) {
        requireLeader(actor);
        updateFields(name, description, maxMembers);
    }

    public RolePartyInvitation invite(Long actor, Long invitee, Instant now) {
        requireLeader(actor);
        if (invitee == null || invitee <= 0 || hasMember(invitee)) throw error(SocialError.ROLE_PARTY_ALREADY_MEMBER);
        RolePartyInvitation existing = invitations.stream().filter(i -> i.getInviteePlayerId().equals(invitee)).findFirst().orElse(null);
        if (existing != null) {
            existing.resend(actor, now);
            return existing;
        }
        RolePartyInvitation invitation = new RolePartyInvitation(this, actor, invitee, now.plusSeconds(7 * 24 * 3600));
        invitations.add(invitation);
        return invitation;
    }

    public void accept(Long actor, Long invitationId, Instant now) {
        RolePartyInvitation invitation = ownInvitation(actor, invitationId);
        if (invitation.getStatus() == RolePartyInvitation.Status.ACCEPTED && hasMember(actor)) return;
        requireActive();
        invitation.requirePending(now);
        if (memberCount() >= maxMembers) throw error(SocialError.ROLE_PARTY_FULL);
        RolePartyMember member = members.stream().filter(m -> m.getPlayerId().equals(actor)).findFirst().orElse(null);
        if (member == null) members.add(new RolePartyMember(this, actor, now));
        else member.rejoin(now);
        invitation.accept();
    }

    public void decline(Long actor, Long invitationId, Instant now) {
        RolePartyInvitation invitation = ownInvitation(actor, invitationId);
        if (invitation.getStatus() == RolePartyInvitation.Status.DECLINED) return;
        requireActive();
        invitation.requirePending(now);
        invitation.decline();
    }

    public void cancelInvitation(Long actor, Long invitationId) {
        requireLeader(actor);
        RolePartyInvitation invitation = findInvitation(invitationId);
        invitation.cancel();
    }

    public void leave(Long actor) {
        requireActive();
        if (leaderPlayerId.equals(actor)) throw error(SocialError.ROLE_PARTY_LEADER_MUST_TRANSFER);
        member(actor).leave();
    }

    public void transferLeader(Long actor, Long target) {
        requireLeader(actor);
        if (actor.equals(target)) throw error(SocialError.ROLE_PARTY_INVALID_STATE);
        member(target);
        leaderPlayerId = target;
    }

    public void disband(Long actor) {
        requireLeader(actor);
        status = Status.DISBANDED;
        invitations.stream().filter(i -> i.getStatus() == RolePartyInvitation.Status.PENDING).forEach(RolePartyInvitation::cancel);
    }

    public void requireMember(Long actor) { member(actor); }
    public boolean hasMember(Long playerId) { return members.stream().anyMatch(m -> m.getPlayerId().equals(playerId) && m.isActive()); }
    public int memberCount() { return (int) members.stream().filter(RolePartyMember::isActive).count(); }
    public List<RolePartyMember> activeMembers() { return members.stream().filter(RolePartyMember::isActive).toList(); }

    private RolePartyMember member(Long actor) {
        return members.stream().filter(m -> m.getPlayerId().equals(actor) && m.isActive()).findFirst()
                .orElseThrow(() -> error(SocialError.ROLE_PARTY_NOT_FOUND));
    }

    private RolePartyInvitation ownInvitation(Long actor, Long id) {
        RolePartyInvitation invitation = findInvitation(id);
        if (!invitation.getInviteePlayerId().equals(actor)) throw error(SocialError.ROLE_PARTY_INVITATION_NOT_FOUND);
        return invitation;
    }

    private RolePartyInvitation findInvitation(Long id) {
        return invitations.stream().filter(i -> i.getId().equals(id)).findFirst()
                .orElseThrow(() -> error(SocialError.ROLE_PARTY_INVITATION_NOT_FOUND));
    }

    public void requireLeader(Long actor) {
        requireActive();
        if (!leaderPlayerId.equals(actor) || !hasMember(actor)) throw error(SocialError.ROLE_PARTY_NOT_FOUND);
    }

    private void requireActive() {
        if (status != Status.ACTIVE) throw error(SocialError.ROLE_PARTY_INVALID_STATE);
    }

    private void updateFields(String name, String description, int maxMembers) {
        if (name == null || name.isBlank() || name.strip().length() > 120 ||
                (description != null && description.strip().length() > 1000) ||
                maxMembers < 2 || maxMembers > 50 || maxMembers < memberCount()) {
            throw error(SocialError.ROLE_PARTY_INVALID_INPUT);
        }
        this.name = name.strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.maxMembers = maxMembers;
    }

    private static DomainException error(SocialError code) { return new DomainException(code); }
}
