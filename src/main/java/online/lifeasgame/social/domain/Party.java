package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AggregateRoot
@Entity(name = "Party")
@Table(name = "parties")
public class Party extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    // 소유자(간접참조): 파티 리더 == 생성자
    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "leader_player_id", nullable = false)
    private Long leaderPlayerId;

    @Embedded
    private PartyName name;

    @Embedded
    private PartyCode code;

    @Embedded
    private PartyDescription description;

    @Embedded
    private PartyBanner banner;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private PartyVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false, length = 32)
    private PartyJoinPolicy joinPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PartyStatus status;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "party_tags", joinColumns = @JoinColumn(name = "party_id"))
    @Column(name = "tag_value", nullable = false, length = 64)
    @BatchSize(size = 100)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<PartyMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<PartyWaitMember> waitMembers = new ArrayList<>();

    public Party(
            Long playerId,
            Long leaderPlayerId,
            PartyName name,
            PartyCode code,
            PartyDescription description,
            PartyBanner banner,
            PartyVisibility visibility,
            PartyJoinPolicy joinPolicy,
            PartyStatus status,
            int maxMembers,
            Set<String> tags,
            List<PartyMember> members,
            List<PartyWaitMember> waitMembers
    ) {
        this.playerId = playerId;
        this.leaderPlayerId = leaderPlayerId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.banner = banner;
        this.visibility = visibility;
        this.joinPolicy = joinPolicy;
        this.status = status;
        this.maxMembers = maxMembers;
        this.tags = tags;
        this.members = members;
        this.waitMembers = waitMembers;
    }

    public static Party create(
            Long playerId,
            String name,
            String code,
            String descriptionMd,
            String bannerImageUrl,
            String bannerBgColor,
            PartyVisibility visibility,
            PartyJoinPolicy joinPolicy,
            int maxMembers
    ) {
        Guard.notNull(playerId, "playerId");
        Guard.notBlank(name, "name");
        Guard.notBlank(code, "code");
        Guard.minValue(maxMembers, 1, "maxMembers");
        Guard.notNull(visibility, "visibility");
        Guard.notNull(joinPolicy, "joinPolicy");

        Party party = new Party(
                playerId,
                playerId,
                PartyName.of(name),
                PartyCode.of(code),
                PartyDescription.of(descriptionMd),
                PartyBanner.of(bannerImageUrl, bannerBgColor),
                visibility ,
                joinPolicy,
                PartyStatus.ACTIVE,
                maxMembers,
                null,
                null,
                null
        );

        PartyMember leader = PartyMember.createLeader(party, playerId);
        party.members.add(leader);

        return party;
    }

    public Optional<PartyMember> findMember(Long playerId) {
        return members.stream()
                .filter(m -> m.getPlayerId().equals(playerId))
                .findFirst();
    }

    public Optional<PartyWaitMember> findPendingJoin(Long playerId) {
        return waitMembers.stream().filter(
                w -> w.getPlayerId().equals(playerId) &&
                        w.getType() == PartyWaitType.JOIN_REQUEST &&
                        w.getStatus() == PartyWaitStatus.PENDING
        ).findFirst();
    }

    public Optional<PartyWaitMember> findPendingInvite(Long playerId) {
        return waitMembers.stream()
                .filter(w -> w.getPlayerId().equals(playerId) &&
                        w.getType() == PartyWaitType.INVITATION &&
                        w.getStatus() == PartyWaitStatus.PENDING
                ).findFirst();
    }

    public int memberCount() {
        return (int) members.stream().count();
    }

    public void rename(String newName) {
        Guard.notBlank(newName, "newName");
        this.name = PartyName.of(newName);
    }

    public void changeVisibility(PartyVisibility partyVisibility) {
        Guard.notNull(partyVisibility, "visibility");
        this.visibility = partyVisibility;
    }

    public void changeJoinPolicy(PartyJoinPolicy partyJoinPolicy) {
        Guard.notNull(partyJoinPolicy, "joinPolicy");
        this.joinPolicy = partyJoinPolicy;
    }

    public void changeMaxMembers(int max) {
        Guard.minValue(max, 1, "maxMembers");
        Guard.check(memberCount() <= max, "maxMembers must be >= current members");
        this.maxMembers = max;
    }

    public void updateDescription(String md) {
        this.description = PartyDescription.of(md);
    }

    public void updateBanner(String imageUrl, String bgColor) {
        this.banner = PartyBanner.of(imageUrl, bgColor);
    }

    public void addTag(String tag) {
        Guard.notBlank(tag, "tag");
        tags.add(tag.trim());
    }

    public void removeTag(String tag) {
        Guard.notBlank(tag, "tag");
        tags.remove(tag.trim());
    }

    public void requestJoin(Long applicantPlayerId, String message) {
        Guard.notNull(applicantPlayerId, "applicantPlayerId");
        Guard.checkState(status == PartyStatus.ACTIVE, "party not active");
        Guard.checkState(joinPolicy != PartyJoinPolicy.INVITE_ONLY, "invite only");
        Guard.check(findMember(applicantPlayerId).isEmpty(), "already member");
        Guard.check(findPendingJoin(applicantPlayerId).isEmpty(), "already requested");

        PartyWaitMember wait = PartyWaitMember.joinRequest(this, applicantPlayerId, message);
        waitMembers.add(wait);
        if (joinPolicy == PartyJoinPolicy.OPEN) {
            approveJoin(applicantPlayerId);
        }
    }

    public void cancelJoinRequest(Long playerId) {
        PartyWaitMember wait = findPendingJoin(playerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.cancel();
    }

    public void approveJoin(Long applicantPlayerId) {
        Guard.check(memberCount() < maxMembers, "capacity exceeded");
        PartyWaitMember wait = findPendingJoin(applicantPlayerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.approve();
        Guard.check(findMember(applicantPlayerId).isEmpty(), "already member");

        PartyMember partyMember = PartyMember.createMember(this, applicantPlayerId);
        members.add(partyMember);
    }

    public void rejectJoin(Long applicantPlayerId) {
        PartyWaitMember wait = findPendingJoin(applicantPlayerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.reject();
    }

    public void invite(Long inviterPlayerId, Long inviteePlayerId, String message, LocalDateTime expiresAt) {
        Guard.notNull(inviterPlayerId, "inviterPlayerId");
        Guard.notNull(inviteePlayerId, "inviteePlayerId");
        Guard.checkState(status == PartyStatus.ACTIVE, "party not active");
        Guard.check(findMember(inviterPlayerId).isPresent(), "inviter must be member");
        Guard.check(findMember(inviteePlayerId).isEmpty(), "invitee already member");
        Guard.check(findPendingInvite(inviteePlayerId).isEmpty(), "already invited");

        PartyWaitMember invitation = PartyWaitMember.invitation(this, inviteePlayerId, message, expiresAt);
        waitMembers.add(invitation);
    }

    public void acceptInvitation(Long playerId) {
        Guard.check(memberCount() < maxMembers, "capacity exceeded");
        PartyWaitMember inv = findPendingInvite(playerId)
                .orElseThrow(() -> new IllegalStateException("invitation not found"));

        if (inv.getExpiresAt() != null) {
            Guard.checkState(!inv.isExpired(), "invitation expired");
        }
        inv.approve();

        Guard.check(findMember(playerId).isEmpty(), "already member");
        PartyMember partyMember = PartyMember.createMember(this, playerId);
        members.add(partyMember);
    }

    public void declineInvitation(Long playerId) {
        PartyWaitMember inv = findPendingInvite(playerId)
                .orElseThrow(() -> new IllegalStateException("invitation not found"));
        inv.reject();
    }

    public void transferLeadership(Long fromLeaderPlayerId, Long toPlayerId) {
        Guard.notNull(fromLeaderPlayerId, "fromLeader");
        Guard.notNull(toPlayerId, "toPlayerId");
        Guard.check(fromLeaderPlayerId.equals(leaderPlayerId), "only current leader can transfer");

        PartyMember from = findMember(fromLeaderPlayerId).orElseThrow();
        PartyMember to = findMember(toPlayerId)
                .orElseThrow(() -> new IllegalStateException("target not member"));

        from.changeRole(PartyMemberRole.MEMBER);
        to.changeRole(PartyMemberRole.LEADER);
        this.leaderPlayerId = toPlayerId;
        this.playerId = toPlayerId;
    }

    public void promoteOfficer(Long actorId, Long targetPlayerId) {
        ensureLeaderOrOfficer(actorId);
        PartyMember target = findMember(targetPlayerId).orElseThrow();
        target.changeRole(PartyMemberRole.OFFICER);
    }

    public void demoteToMember(Long actorId, Long targetPlayerId) {
        ensureLeaderOrOfficer(actorId);
        PartyMember target = findMember(targetPlayerId).orElseThrow();
        Guard.checkState(!target.getRole().equals(PartyMemberRole.LEADER), "leader cannot be demoted");
        target.changeRole(PartyMemberRole.MEMBER);
    }

    public void kickMember(Long targetPlayerId) {
        PartyMember target = findMember(targetPlayerId).orElseThrow();
        Guard.checkState(!target.getRole().equals(PartyMemberRole.LEADER), "cannot kick leader");
        members.remove(target);
    }

    public void leave(Long playerId) {
        PartyMember me = findMember(playerId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        Guard.checkState(!me.getRole().equals(PartyMemberRole.LEADER), "leader cannot leave");
        members.remove(me);
    }

    public void disbandByLeader(Long leaderId) {
        Guard.check(leaderId.equals(leaderPlayerId), "only leader");
        this.status = PartyStatus.DISBANDED;
        members.clear();
        waitMembers.clear();
    }

    private void ensureLeaderOrOfficer(Long actorId) {
        PartyMember me = findMember(actorId)
                .orElseThrow(() -> new IllegalStateException("not member"));
        Guard.checkState(
                me.getRole() == PartyMemberRole.LEADER || me.getRole() == PartyMemberRole.OFFICER,
                "officer or leader only"
        );
    }
}
