package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.time.CalendarDateRange;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildJoinPolicy;
import online.lifeasgame.social.domain.GuildMemberRole;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.GuildWaitType;
import online.lifeasgame.social.domain.GuildStatus;
import online.lifeasgame.social.domain.repository.GuildRepository;
import online.lifeasgame.demo.application.DemoActorScopeApi;
import online.lifeasgame.social.infra.GroupRosterStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.ArrayList;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuildService {

    private final GuildReader guildReader;
    private final GuildWriter guildWriter;
    private final GuildRepository repository;
    private final GroupRosterStore roster;
    private final DemoActorScopeApi demoActorScope;

    @Transactional
    public GuildResult.Info create(Long playerId, GuildCommand.Create command) {
        Guild guild = guildWriter.create(
                Guild.create(
                        playerId,
                        command.name(),
                        command.code(),
                        command.descriptionMd(),
                        command.emblemImageUrl(),
                        command.emblemBgColor(),
                        command.visibility() == null ? null : GuildVisibility.valueOf(command.visibility()),
                        command.joinPolicy() == null ? null : GuildJoinPolicy.valueOf(command.joinPolicy()),
                        command.maxMembers()
                )
        );
        
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info rename(Long playerId, Long id, GuildCommand.Rename command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        guild.rename(command.name());
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info changePolicy(Long playerId, Long id, GuildCommand.ChangePolicy command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        
        if (command.visibility() != null) {
            guild.changeVisibility(GuildVisibility.valueOf(command.visibility()));
        }
        if (command.joinPolicy() != null) {
            guild.changeJoinPolicy(GuildJoinPolicy.valueOf(command.joinPolicy()));
        }
        if (command.maxMembers() > 0) {
            guild.changeMaxMembers(command.maxMembers());
        }
        
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info changeDescription(Long playerId, Long id, GuildCommand.ChangeDescription command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        guild.updateDescription(command.descriptionMd());
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info changeEmblem(Long playerId, Long id, GuildCommand.ChangeEmblem command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        guild.updateEmblem(command.emblemImageUrl(), command.emblemBgColor());
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info addTag(Long playerId, Long id, GuildCommand.TagOp command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        guild.addTag(command.tag());
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public GuildResult.Info removeTag(Long playerId, Long id, GuildCommand.TagOp command) {
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        guild.removeTag(command.tag());
        return GuildResult.Info.from(guild);
    }

    @Transactional
    public void requestJoin(Long playerId, Long id, GuildCommand.RequestJoin command) {
        Guild guild = locked(id);
        guild.requestJoin(playerId, command.message());
    }

    @Transactional
    public void approveJoin(Long playerId, Long id, GuildCommand.Approve command) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.approveJoin(command.applicantPlayerId());
    }

    @Transactional
    public void rejectJoin(Long playerId, Long id, GuildCommand.Reject command) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.rejectJoin(command.applicantPlayerId());
    }

    @Transactional
    public void cancelJoin(Long playerId, Long id) {
        Guild guild = locked(id);
        guild.cancelJoinRequest(playerId);
    }

    @Transactional
    public void transferLeader(Long playerId, Long id, GuildCommand.TransferLeader command) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.transferLeadership(guild.getLeaderPlayerId(), command.toPlayerId());
        roster.cancelPending(GroupRosterStore.Type.GUILD, id, java.time.Instant.now());
    }

    @Transactional
    public void kick(Long playerId, Long id, GuildCommand.Kick command) {
        Guild guild = locked(id);
        ensureLeaderOrOfficer(guild, playerId);
        guild.kickMember(command.targetPlayerId());
    }

    @Transactional
    public void promote(Long playerId, Long id, GuildCommand.Promote command) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.promoteOfficer(guild.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void demote(Long playerId, Long id, GuildCommand.Demote command) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.demoteToMember(guild.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void leave(Long playerId, Long id) {
        Guild guild = locked(id);
        guild.leave(playerId);
    }

    @Transactional
    public void disbandByLeader(Long playerId, Long id) {
        Guild guild = locked(id);
        ensureLeader(guild, playerId);
        guild.disbandByLeader(guild.getLeaderPlayerId());
        roster.cancelPending(GroupRosterStore.Type.GUILD, id, java.time.Instant.now());
    }

    @Transactional
    public void invite(Long playerId, Long id, GuildCommand.Invite command) {
        demoActorScope.requireSameBoundary(playerId, command.inviteePlayerId());
        Guild guild = locked(id);
        ensureLeaderOrOfficer(guild, playerId);
        guild.invite(playerId, command.inviteePlayerId(), command.message(), parseDateTime(command.expiresAtIso()));
    }

    @Transactional
    public void acceptInvitation(Long playerId, Long id) {
        Guild guild = locked(id);
        guild.acceptInvitation(playerId);
    }

    @Transactional
    public void declineInvitation(Long playerId, Long id) {
        Guild guild = locked(id);
        guild.declineInvitation(playerId);
    }

    public GuildResult.Page<GuildResult.Summary> search(String keyword, String visibility, int page, int size) {
        List<Guild> guilds = guildReader.search(keyword, visibility, page, size);
        long total = guildReader.countSearch(keyword, visibility);
        List<GuildResult.Summary> contents = guilds.stream()
                .map(GuildResult.Summary::from)
                .toList();
        return GuildResult.Page.of(contents, page, size, total);
    }

    public List<GuildResult.Summary> recent(int limit) {
        return guildReader.recent(limit).stream()
                .map(GuildResult.Summary::from)
                .toList();
    }

    public GuildResult.Info getGuild(Long playerId, Long id) {
        Guild group = guildReader.getByIdOrThrow(id);
        requireMember(group, playerId);
        return GuildResult.Info.from(group);
    }

    public GuildResult.Summary preview(Long id) {
        Guild group = guildReader.getByIdOrThrow(id);
        if (group.getVisibility() != GuildVisibility.PUBLIC || group.getStatus() != GuildStatus.ACTIVE) {
            throw new DomainException(SocialError.GUILD_NOT_FOUND);
        }
        return GuildResult.Summary.from(group);
    }

    public GuildResult.Page<GuildResult.MyGuild> mine(Long playerId, int page, int size) {
        var rows = repository.findMine(playerId, pageOf(page, size));
        return GuildResult.Page.of(rows.stream().map(GuildResult.MyGuild::from).toList(),
                page, size, rows.getTotalElements());
    }

    public GuildResult.Page<GuildResult.Member> members(Long playerId, Long id, int page, int size) {
        requireMember(guildReader.getByIdOrThrow(id), playerId);
        var rows = repository.findMembers(id, pageOf(page, size));
        return GuildResult.Page.of(rows.stream().map(GuildResult.Member::from).toList(), page, size, rows.getTotalElements());
    }

    public GuildResult.Page<GuildResult.Pending> pendingRequests(Long playerId, Long id, int page, int size) {
        Guild group = guildReader.getByIdOrThrow(id);
        ensureLeader(group, playerId);
        var rows = repository.findPendingRequests(id, pageOf(page, size));
        return GuildResult.Page.of(rows.stream().map(GuildResult.Pending::from).toList(), page, size, rows.getTotalElements());
    }

    public GuildResult.Page<GuildResult.Pending> myPending(Long playerId, GuildWaitType type, int page, int size) {
        var rows = repository.findMyPending(playerId, type, pageOf(page, size));
        return GuildResult.Page.of(rows.stream().map(GuildResult.Pending::from).toList(), page, size, rows.getTotalElements());
    }

    public GuildResult.Me me(Long playerId, Long id) {
        Guild group = guildReader.getByIdOrThrow(id);
        var membership = group.findMember(playerId);
        var invitation = group.findPendingInvite(playerId);
        boolean invited = invitation.isPresent();
        if (membership.isEmpty() && !invited && (group.getVisibility() != GuildVisibility.PUBLIC || group.getStatus() != GuildStatus.ACTIVE)) {
            throw new DomainException(SocialError.GUILD_NOT_FOUND);
        }
        boolean requested = group.findPendingJoin(playerId).isPresent();
        List<String> actions = new ArrayList<>();
        if (group.getStatus() == GuildStatus.ACTIVE) {
            if (membership.isPresent()) {
                String role = membership.get().getRole().name();
                if (role.equals("LEADER")) {
                    actions.addAll(List.of("rename", "policy", "description", "emblem", "tags/add", "tags/remove",
                            "approve", "reject", "transfer-leader", "promote", "demote", "disband"));
                } else {
                    actions.add("leave");
                }
                if (role.equals("LEADER") || role.equals("OFFICER")) actions.addAll(List.of("invite", "kick"));
            } else if (invited) {
                if (!invitation.orElseThrow().isExpired()) actions.add("accept-invitation");
                actions.add("decline-invitation");
            } else if (requested) {
                actions.add("cancel-join");
            } else if (group.getVisibility() == GuildVisibility.PUBLIC && group.getJoinPolicy() != online.lifeasgame.social.domain.GuildJoinPolicy.INVITE_ONLY
                    && group.memberCount() < group.getMaxMembers()) {
                actions.add("request-join");
            }
        }
        return new GuildResult.Me(membership.map(m -> m.getRole().name()).orElse(null), requested, invited, actions);
    }

    private static PageRequest pageOf(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new DomainException(SocialError.INVALID_STATE);
        return PageRequest.of(page, size);
    }

    private Guild locked(Long id) {
        return repository.findForUpdate(id).orElseThrow(() -> new DomainException(SocialError.GUILD_NOT_FOUND));
    }

    private static void requireMember(Guild group, Long playerId) {
        if (group.findMember(playerId).isEmpty()) throw new DomainException(SocialError.GUILD_NOT_FOUND);
    }

    private static void ensureLeader(Guild guild, Long actorId) {
        var me = guild.findMember(actorId)
                .orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() != GuildMemberRole.LEADER) {
            throw new DomainException(SocialError.LEADER_ONLY);
        }
    }

    private static void ensureLeaderOrOfficer(Guild guild, Long actorId) {
        var me = guild.findMember(actorId)
                .orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() == GuildMemberRole.MEMBER) {
            throw new DomainException(SocialError.OFFICER_OR_LEADER_ONLY);
        }
    }

    private static LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }

        try {
            return CalendarDateRange.parseDateTimeColumn(iso);
        } catch (DateTimeParseException e) {
            throw new DomainException(SocialError.INVALID_STATE, "INVALID_EXPIRES_AT");
        }
    }
}
