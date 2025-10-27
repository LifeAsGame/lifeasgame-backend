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
@Entity(name = "Guild")
@Table(name = "guilds")
public class Guild extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guild_id")
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "leader_player_id", nullable = false)
    private Long leaderPlayerId;

    @Embedded
    private GuildName name;

    @Embedded
    private GuildCode code;

    @Embedded
    private GuildDescription description;

    @Embedded
    private GuildEmblem emblem;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private GuildVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_policy", nullable = false, length = 32)
    private GuildJoinPolicy joinPolicy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GuildStatus status;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "guild_tags", joinColumns = @JoinColumn(name = "guild_id"))
    @Column(name = "tag_value", nullable = false, length = 64)
    @BatchSize(size = 100)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<GuildMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "guild", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<GuildWaitMember> waitMembers = new ArrayList<>();

    public static Guild create(
            Long playerId,
            String name,
            String code,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
            GuildVisibility visibility,
            GuildJoinPolicy joinPolicy,
            int maxMembers
    ) {
        Guard.notNull(playerId, "playerId");
        Guard.notBlank(name, "name");
        Guard.notBlank(code, "code");
        Guard.minValue(maxMembers, 1, "maxMembers");
        Guard.notNull(visibility, "visibility");
        Guard.notNull(joinPolicy, "joinPolicy");

        Guild g = new Guild();
        g.playerId = playerId;
        g.name = GuildName.of(name);
        g.code = GuildCode.of(code);
        g.description = GuildDescription.of(descriptionMd);
        g.emblem = GuildEmblem.of(emblemImageUrl, emblemBgColor);
        g.visibility = visibility;
        g.joinPolicy = joinPolicy;
        g.status = GuildStatus.ACTIVE;
        g.maxMembers = maxMembers;

        GuildMember leader = GuildMember.createLeader(g, playerId);
        g.members.add(leader);
        g.leaderPlayerId = playerId;

        return g;
    }

    // ===== 조회 유틸 =====
    public Optional<GuildMember> findMember(Long playerId) {
        return members.stream().filter(m -> m.getPlayerId().equals(playerId)).findFirst();
    }

    public Optional<GuildWaitMember> findPendingJoin(Long playerId) {
        return waitMembers.stream().filter(w -> w.getPlayerId().equals(playerId) &&
                w.getType() == GuildWaitType.JOIN_REQUEST &&
                w.getStatus() == GuildWaitStatus.PENDING).findFirst();
    }

    public Optional<GuildWaitMember> findPendingInvite(Long playerId) {
        return waitMembers.stream()
                .filter(
                        w -> w.getPlayerId().equals(playerId) &&
                                w.getType() == GuildWaitType.INVITATION &&
                                w.getStatus() == GuildWaitStatus.PENDING
                ).findFirst();
    }

    public int memberCount() {
        return members.size();
    }

    // ===== 기본 변경 =====
    public void rename(String newName) {
        Guard.notBlank(newName, "newName");
        this.name = GuildName.of(newName);
    }

    public void changeVisibility(GuildVisibility v) {
        Guard.notNull(v, "visibility");
        this.visibility = v;
    }

    public void changeJoinPolicy(GuildJoinPolicy p) {
        Guard.notNull(p, "joinPolicy");
        this.joinPolicy = p;
    }

    public void changeMaxMembers(int max) {
        Guard.minValue(max, 1, "maxMembers must be positive");
        Guard.check(memberCount() <= max, "maxMembers must be >= current members");
        this.maxMembers = max;
    }

    public void updateDescription(String md) {
        this.description = GuildDescription.of(md);
    }

    public void updateEmblem(String imageUrl, String bgColor) {
        this.emblem = GuildEmblem.of(imageUrl, bgColor);
    }

    public void addTag(String tag) {
        Guard.notBlank(tag, "tag");
        this.tags.add(tag.trim());
    }

    public void removeTag(String tag) {
        Guard.notBlank(tag, "tag");
        this.tags.remove(tag.trim());
    }

    // ===== 가입/초대 =====
    public void requestJoin(Long applicantPlayerId, String message) {
        Guard.notNull(applicantPlayerId, "applicantPlayerId");
        Guard.checkState(status == GuildStatus.ACTIVE, "guild not active");
        Guard.checkState(joinPolicy != GuildJoinPolicy.INVITE_ONLY, "invite only");
        Guard.check(findMember(applicantPlayerId).isEmpty(), "already member");
        Guard.check(findPendingJoin(applicantPlayerId).isEmpty(), "already requested");

        GuildWaitMember wait = GuildWaitMember.joinRequest(this, applicantPlayerId, message);
        this.waitMembers.add(wait);
        if (joinPolicy == GuildJoinPolicy.OPEN) {
            approveJoin(applicantPlayerId);
        }
    }

    public void cancelJoinRequest(Long playerId) {
        GuildWaitMember wait = findPendingJoin(playerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.cancel();
    }

    public void approveJoin(Long applicantPlayerId) {
        Guard.check(memberCount() < maxMembers, "capacity exceeded");
        GuildWaitMember wait = findPendingJoin(applicantPlayerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.approve();
        Guard.check(findMember(applicantPlayerId).isEmpty(), "already member");
        this.members.add(GuildMember.createMember(this, applicantPlayerId));
    }

    public void rejectJoin(Long applicantPlayerId) {
        GuildWaitMember wait = findPendingJoin(applicantPlayerId)
                .orElseThrow(() -> new IllegalStateException("join request not found"));
        wait.reject();
    }

    public void invite(Long inviterPlayerId, Long inviteePlayerId, String message, LocalDateTime expiresAt) {
        Guard.notNull(inviterPlayerId, "inviterPlayerId");
        Guard.notNull(inviteePlayerId, "inviteePlayerId");
        Guard.checkState(status == GuildStatus.ACTIVE, "guild not active");
        Guard.check(findMember(inviterPlayerId).isPresent(), "inviter must be member");
        Guard.check(findMember(inviteePlayerId).isEmpty(), "invitee already member");
        Guard.check(findPendingInvite(inviteePlayerId).isEmpty(), "already invited");

        GuildWaitMember inv = GuildWaitMember.invitation(this, inviteePlayerId, message, expiresAt);
        this.waitMembers.add(inv);
    }

    public void acceptInvitation(Long playerId) {
        Guard.check(memberCount() < maxMembers, "capacity exceeded");
        GuildWaitMember inv = findPendingInvite(playerId)
                .orElseThrow(() -> new IllegalStateException("invitation not found"));
        if (inv.getExpiresAt() != null) {
            Guard.checkState(!inv.isExpired(), "invitation expired");
        }
        inv.approve();
        Guard.check(findMember(playerId).isEmpty(), "already member");
        this.members.add(GuildMember.createMember(this, playerId));
    }

    public void declineInvitation(Long playerId) {
        GuildWaitMember inv = findPendingInvite(playerId)
                .orElseThrow(() -> new IllegalStateException("invitation not found"));
        inv.reject();
    }

    // ===== 멤버/권한 =====
    public void transferLeadership(Long fromLeaderPlayerId, Long toPlayerId) {
        Guard.notNull(fromLeaderPlayerId, "fromLeader");
        Guard.notNull(toPlayerId, "toPlayerId");
        Guard.check(fromLeaderPlayerId.equals(leaderPlayerId), "only current leader can transfer");

        GuildMember from = findMember(fromLeaderPlayerId).orElseThrow();
        GuildMember to = findMember(toPlayerId)
                .orElseThrow(() -> new IllegalStateException("target not member"));

        from.changeRole(GuildMemberRole.MEMBER);
        to.changeRole(GuildMemberRole.LEADER);
        this.leaderPlayerId = toPlayerId;
        this.playerId = toPlayerId;
    }

    public void promoteOfficer(Long actorId, Long targetPlayerId) {
        ensureLeaderOrOfficer(actorId);
        GuildMember target = findMember(targetPlayerId).orElseThrow();
        target.changeRole(GuildMemberRole.OFFICER);
    }

    public void demoteToMember(Long actorId, Long targetPlayerId) {
        ensureLeaderOrOfficer(actorId);
        GuildMember target = findMember(targetPlayerId).orElseThrow();
        Guard.checkState(!target.getRole().equals(GuildMemberRole.LEADER), "leader cannot be demoted");
        target.changeRole(GuildMemberRole.MEMBER);
    }

    public void kickMember(Long targetPlayerId) {
        GuildMember target = findMember(targetPlayerId).orElseThrow();
        Guard.checkState(!target.getRole().equals(GuildMemberRole.LEADER), "cannot kick leader");
        this.members.remove(target);
    }

    public void leave(Long playerId) {
        GuildMember me = findMember(playerId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        Guard.checkState(!me.getRole().equals(GuildMemberRole.LEADER), "leader cannot leave");
        this.members.remove(me);
    }

    public void disbandByLeader(Long leaderId) {
        Guard.check(leaderId.equals(leaderPlayerId), "only leader");
        this.status = GuildStatus.DISBANDED;
        this.members.clear();
        this.waitMembers.clear();
    }

    private void ensureLeaderOrOfficer(Long actorId) {
        GuildMember me = findMember(actorId)
                .orElseThrow(() -> new IllegalStateException("not member"));
        Guard.checkState(
                me.getRole() == GuildMemberRole.LEADER || me.getRole() == GuildMemberRole.OFFICER,
                "officer or leader only"
        );
    }
}
