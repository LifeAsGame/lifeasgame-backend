package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildJoinPolicy;
import online.lifeasgame.social.domain.GuildMemberRole;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class GuildService {

    private final GuildReader guildReader;
    private final GuildWriter guildWriter;

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
        Guild guild = guildReader.getByIdOrThrow(id);
        guild.requestJoin(playerId, command.message());
    }

    @Transactional
    public void approveJoin(Long playerId, Long id, GuildCommand.Approve command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.approveJoin(command.applicantPlayerId());
    }

    @Transactional
    public void rejectJoin(Long playerId, Long id, GuildCommand.Reject command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.rejectJoin(command.applicantPlayerId());
    }

    @Transactional
    public void cancelJoin(Long playerId, Long id) {
        Guild guild = guildReader.getByIdOrThrow(id);
        guild.cancelJoinRequest(playerId);
    }

    @Transactional
    public void transferLeader(Long playerId, Long id, GuildCommand.TransferLeader command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.transferLeadership(command.fromLeaderPlayerId(), command.toPlayerId());
    }

    @Transactional
    public void kick(Long playerId, Long id, GuildCommand.Kick command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeaderOrOfficer(guild, playerId);
        guild.kickMember(command.targetPlayerId());
    }

    @Transactional
    public void promote(Long playerId, Long id, GuildCommand.Promote command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.promoteOfficer(guild.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void demote(Long playerId, Long id, GuildCommand.Demote command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.demoteToMember(guild.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void leave(Long playerId, Long id) {
        Guild guild = guildReader.getByIdOrThrow(id);
        guild.leave(playerId);
    }

    @Transactional
    public void disbandByLeader(Long playerId, Long id) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeader(guild, playerId);
        guild.disbandByLeader(guild.getLeaderPlayerId());
    }

    @Transactional
    public void invite(Long playerId, Long id, GuildCommand.Invite command) {
        Guild guild = guildReader.getByIdOrThrow(id);
        ensureLeaderOrOfficer(guild, playerId);
        guild.invite(playerId, command.inviteePlayerId(), command.message(), parseDateTime(command.expiresAtIso()));
    }

    @Transactional
    public void acceptInvitation(Long playerId, Long id) {
        Guild guild = guildReader.getByIdOrThrow(id);
        guild.acceptInvitation(playerId);
    }

    @Transactional
    public void declineInvitation(Long playerId, Long id) {
        Guild guild = guildReader.getByIdOrThrow(id);
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
        Guild guild = guildReader.getByPlayerIdAndIdOrThrow(playerId, id);
        return GuildResult.Info.from(guild);
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
            return LocalDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            throw new DomainException(SocialError.INVALID_STATE, "INVALID_EXPIRES_AT");
        }
    }
}
