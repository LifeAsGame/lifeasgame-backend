package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.model.GuildSpec;
import online.lifeasgame.social.application.result.GuildResult;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildMemberRole;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class GuildService {

    private final GuildReader guildReader;
    private final GuildWriter guildWriter;

    @Transactional
    public GuildResult.Info create(Long playerId, GuildCommand.Create c) {
        Guild saved = guildWriter.create(GuildSpec.Create.from(playerId, c));
        return GuildResult.Info.from(saved);
    }

    @Transactional
    public GuildResult.Info rename(Long playerId, Long id, GuildCommand.Rename c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.rename(g, c));
    }

    @Transactional
    public GuildResult.Info changePolicy(Long playerId, Long id, GuildCommand.ChangePolicy c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.changePolicy(g, c));
    }

    @Transactional
    public GuildResult.Info changeDescription(Long playerId, Long id, GuildCommand.ChangeDescription c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.changeDescription(g, c));
    }

    @Transactional
    public GuildResult.Info changeEmblem(Long playerId, Long id, GuildCommand.ChangeEmblem c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.changeEmblem(g, c));
    }

    @Transactional
    public GuildResult.Info addTag(Long playerId, Long id, GuildCommand.TagOp c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.addTag(g, c));
    }

    @Transactional
    public GuildResult.Info removeTag(Long playerId, Long id, GuildCommand.TagOp c) {
        Guild g = guildReader.getOwned(playerId, id);
        return GuildResult.Info.from(guildWriter.removeTag(g, c));
    }

    // 가입/권한
    @Transactional
    public void requestJoin(Long playerId, Long id, GuildCommand.RequestJoin c) {
        Guild g = guildReader.get(id);
        guildWriter.requestJoin(g, playerId, c);
    }

    @Transactional
    public void approveJoin(Long playerId, Long id, GuildCommand.Approve c) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.approveJoin(g, c);
    }

    @Transactional
    public void rejectJoin(Long playerId, Long id, GuildCommand.Reject c) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.rejectJoin(g, c);
    }

    @Transactional
    public void cancelJoin(Long playerId, Long id) {
        Guild g = guildReader.get(id);
        guildWriter.cancelJoin(g, playerId);
    }

    @Transactional
    public void transferLeader(Long playerId, Long id, GuildCommand.TransferLeader c) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.transferLeader(g, c);
    }

    @Transactional
    public void kick(Long playerId, Long id, GuildCommand.Kick c) {
        Guild g = guildReader.get(id);
        ensureLeaderOrOfficer(g, playerId);
        guildWriter.kick(g, c);
    }

    @Transactional
    public void promote(Long playerId, Long id, GuildCommand.Promote c) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.promote(g, c);
    }

    @Transactional
    public void demote(Long playerId, Long id, GuildCommand.Demote c) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.demote(g, c);
    }

    @Transactional
    public void leave(Long playerId, Long id) {
        Guild g = guildReader.get(id);
        guildWriter.leave(g, playerId);
    }

    @Transactional
    public void disbandByLeader(Long playerId, Long id) {
        Guild g = guildReader.get(id);
        ensureLeader(g, playerId);
        guildWriter.disbandByLeader(g, playerId);
    }

    // 초대
    @Transactional
    public void invite(Long playerId, Long id, GuildCommand.Invite c) {
        Guild g = guildReader.get(id);
        ensureLeaderOrOfficer(g, playerId);
        guildWriter.invite(g, playerId, c);
    }

    @Transactional
    public void acceptInvitation(Long playerId, Long id) {
        Guild g = guildReader.get(id);
        guildWriter.acceptInvitation(g, playerId);
    }

    @Transactional
    public void declineInvitation(Long playerId, Long id) {
        Guild g = guildReader.get(id);
        guildWriter.declineInvitation(g, playerId);
    }

    public GuildResult.Page<GuildResult.Summary> search(String keyword, String visibility, int page, int size) {
        List<Guild> domains = guildReader.search(keyword, visibility, page, size);
        long total = guildReader.countSearch(keyword, visibility);
        List<GuildResult.Summary> contents = domains.stream().map(GuildResult.Summary::from).toList();
        return GuildResult.Page.of(contents, page, size, total);
    }

    public List<GuildResult.Summary> recent(int limit) {
        return guildReader.recent(limit).stream().map(GuildResult.Summary::from).toList();
    }

    public GuildResult.Summary getGuild(Long playerId, Long id) {
        Guild guild = guildReader.getGuild(playerId, id);
        return GuildResult.Summary.from(guild);
    }

    private static void ensureLeader(Guild g, Long actorId) {
        var me = g.findMember(actorId)
                .orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() != GuildMemberRole.LEADER) {
            throw new DomainException(SocialError.LEADER_ONLY);
        }
    }

    private static void ensureLeaderOrOfficer(Guild g, Long actorId) {
        var me = g.findMember(actorId)
                .orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() == GuildMemberRole.MEMBER) {
            throw new DomainException(SocialError.OFFICER_OR_LEADER_ONLY);
        }
    }
}
