package online.lifeasgame.social.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;
import online.lifeasgame.social.domain.error.SocialError;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "guild_group_links", uniqueConstraints = @UniqueConstraint(name = "uq_guild_group_open", columnNames = {"guild_id", "group_type", "group_id", "open_slot"}))
public class GuildGroupLink extends AbstractTime {
    public enum GroupType { PARTY, ROLE_PARTY }
    public enum Status { PENDING, ACTIVE, REJECTED, CANCELED, UNLINKED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "guild_id", nullable = false, updatable = false)
    private Long guildId;
    @Enumerated(EnumType.STRING) @Column(name = "group_type", nullable = false, updatable = false, length = 20)
    private GroupType groupType;
    @Column(name = "group_id", nullable = false, updatable = false)
    private Long groupId;
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status;
    @Column(name = "proposed_by_player_id", nullable = false, updatable = false)
    private Long proposedByPlayerId;
    @Column(name = "guild_approved_by_player_id")
    private Long guildApprovedByPlayerId;
    @Column(name = "group_approved_by_player_id")
    private Long groupApprovedByPlayerId;
    @Column(name = "open_slot")
    private Integer openSlot;
    @Version private long version;

    public static GuildGroupLink propose(Long guildId, GroupType type, Long groupId, String displayName,
                                         Long actor, boolean guildLeader, boolean groupLeader) {
        GuildGroupLink link = new GuildGroupLink();
        link.guildId = guildId;
        link.groupType = type;
        link.groupId = groupId;
        link.displayName = label(displayName);
        link.proposedByPlayerId = actor;
        link.guildApprovedByPlayerId = guildLeader ? actor : null;
        link.groupApprovedByPlayerId = groupLeader ? actor : null;
        link.status = guildLeader && groupLeader ? Status.ACTIVE : Status.PENDING;
        link.openSlot = 1;
        return link;
    }

    public void approve(Long actor, boolean guildLeader, boolean groupLeader, String displayName) {
        if (status != Status.PENDING || (!guildLeader && !groupLeader)) throw new DomainException(SocialError.GUILD_GROUP_CONFLICT);
        requireProposedName(displayName);
        if (guildLeader) guildApprovedByPlayerId = actor;
        if (groupLeader) groupApprovedByPlayerId = actor;
        if (guildApprovedByPlayerId != null && groupApprovedByPlayerId != null) status = Status.ACTIVE;
    }

    public void requireProposedName(String displayName) {
        if (!this.displayName.equals(label(displayName))) throw new DomainException(SocialError.GUILD_GROUP_CONFLICT);
    }

    public void reconcileLeaders(Long currentGuildLeader, Long currentGroupLeader) {
        if (status != Status.PENDING) return;
        if (!currentGuildLeader.equals(guildApprovedByPlayerId)) guildApprovedByPlayerId = null;
        if (!currentGroupLeader.equals(groupApprovedByPlayerId)) groupApprovedByPlayerId = null;
    }

    public void reject() { close(Status.REJECTED); }
    public void cancel() { close(Status.CANCELED); }
    public void unlink() { close(Status.UNLINKED); }

    private void close(Status terminal) {
        if ((terminal == Status.UNLINKED && status != Status.ACTIVE) ||
                (terminal != Status.UNLINKED && status != Status.PENDING))
            throw new DomainException(SocialError.GUILD_GROUP_CONFLICT);
        status = terminal;
        openSlot = null;
    }

    private static String label(String raw) {
        if (raw == null || raw.isBlank() || raw.strip().length() > 120)
            throw new DomainException(SocialError.GUILD_GROUP_INVALID_INPUT);
        return raw.strip();
    }
}
