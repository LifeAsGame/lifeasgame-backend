package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class GuildFacade {

    private final GuildService guildService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    private Long player() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }

    public GuildResult.Info create(GuildCommand.Create c) {
        return guildService.create(player(), c);
    }

    public GuildResult.Info rename(Long guildId, GuildCommand.Rename c) {
        return guildService.rename(player(), guildId, c);
    }

    public GuildResult.Info changePolicy(Long guildId, GuildCommand.ChangePolicy c) {
        return guildService.changePolicy(player(), guildId, c);
    }

    public GuildResult.Info changeDescription(Long guildId, GuildCommand.ChangeDescription c) {
        return guildService.changeDescription(player(), guildId, c);
    }

    public GuildResult.Info changeEmblem(Long guildId, GuildCommand.ChangeEmblem c) {
        return guildService.changeEmblem(player(), guildId, c);
    }

    public GuildResult.Info addTag(Long guildId, GuildCommand.TagOp c) {
        return guildService.addTag(player(), guildId, c);
    }

    public GuildResult.Info removeTag(Long guildId, GuildCommand.TagOp c) {
        return guildService.removeTag(player(), guildId, c);
    }

    public void requestJoin(Long guildId, GuildCommand.RequestJoin c) {
        guildService.requestJoin(player(), guildId, c);
    }

    public void approveJoin(Long guildId, GuildCommand.Approve c) {
        guildService.approveJoin(player(), guildId, c);
    }

    public void rejectJoin(Long guildId, GuildCommand.Reject c) {
        guildService.rejectJoin(player(), guildId, c);
    }

    public void cancelJoin(Long guildId) {
        guildService.cancelJoin(player(), guildId);
    }

    public void transferLeader(Long guildId, GuildCommand.TransferLeader c) {
        guildService.transferLeader(player(), guildId, c);
    }

    public void kick(Long guildId, GuildCommand.Kick c) {
        guildService.kick(player(), guildId, c);
    }

    public void promote(Long guildId, GuildCommand.Promote c) {
        guildService.promote(player(), guildId, c);
    }

    public void demote(Long guildId, GuildCommand.Demote c) {
        guildService.demote(player(), guildId, c);
    }

    public void leave(Long guildId) {
        guildService.leave(player(), guildId);
    }

    public void disband(Long guildId) {
        guildService.disbandByLeader(player(), guildId);
    }

    public void invite(Long guildId, GuildCommand.Invite c) {
        guildService.invite(player(), guildId, c);
    }

    public void acceptInvitation(Long guildId) {
        guildService.acceptInvitation(player(), guildId);
    }

    public void declineInvitation(Long guildId) {
        guildService.declineInvitation(player(), guildId);
    }

    public GuildResult.Page<GuildResult.Summary> search(String keyword, String visibility, int page, int size) {
        return guildService.search(keyword, visibility, page, size);
    }

    public GuildResult.Info getGuild(Long guildId) {
        return guildService.getGuild(player(), guildId);
    }

    public List<GuildResult.Summary> recent(int limit) {
        return guildService.recent(limit);
    }
}
