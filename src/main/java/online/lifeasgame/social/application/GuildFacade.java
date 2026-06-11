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

    public GuildResult.Info create(GuildCommand.Create command) {
        return guildService.create(player(), command);
    }

    public GuildResult.Info rename(Long guildId, GuildCommand.Rename command) {
        return guildService.rename(player(), guildId, command);
    }

    public GuildResult.Info changePolicy(Long guildId, GuildCommand.ChangePolicy command) {
        return guildService.changePolicy(player(), guildId, command);
    }

    public GuildResult.Info changeDescription(Long guildId, GuildCommand.ChangeDescription command) {
        return guildService.changeDescription(player(), guildId, command);
    }

    public GuildResult.Info changeEmblem(Long guildId, GuildCommand.ChangeEmblem command) {
        return guildService.changeEmblem(player(), guildId, command);
    }

    public GuildResult.Info addTag(Long guildId, GuildCommand.TagOp command) {
        return guildService.addTag(player(), guildId, command);
    }

    public GuildResult.Info removeTag(Long guildId, GuildCommand.TagOp command) {
        return guildService.removeTag(player(), guildId, command);
    }

    public void requestJoin(Long guildId, GuildCommand.RequestJoin command) {
        guildService.requestJoin(player(), guildId, command);
    }

    public void approveJoin(Long guildId, GuildCommand.Approve command) {
        guildService.approveJoin(player(), guildId, command);
    }

    public void rejectJoin(Long guildId, GuildCommand.Reject command) {
        guildService.rejectJoin(player(), guildId, command);
    }

    public void cancelJoin(Long guildId) {
        guildService.cancelJoin(player(), guildId);
    }

    public void transferLeader(Long guildId, GuildCommand.TransferLeader command) {
        guildService.transferLeader(player(), guildId, command);
    }

    public void kick(Long guildId, GuildCommand.Kick command) {
        guildService.kick(player(), guildId, command);
    }

    public void promote(Long guildId, GuildCommand.Promote command) {
        guildService.promote(player(), guildId, command);
    }

    public void demote(Long guildId, GuildCommand.Demote command) {
        guildService.demote(player(), guildId, command);
    }

    public void leave(Long guildId) {
        guildService.leave(player(), guildId);
    }

    public void disband(Long guildId) {
        guildService.disbandByLeader(player(), guildId);
    }

    public void invite(Long guildId, GuildCommand.Invite command) {
        guildService.invite(player(), guildId, command);
    }

    public void acceptInvitation(Long guildId) {
        guildService.acceptInvitation(player(), guildId);
    }

    public void declineInvitation(Long guildId) {
        guildService.declineInvitation(player(), guildId);
    }

    public GuildResult.Page<GuildResult.Summary> search(
            String keyword,
            String visibility,
            int page,
            int size
    ) {
        return guildService.search(keyword, visibility, page, size);
    }

    public GuildResult.Info getGuild(Long guildId) {
        return guildService.getGuild(player(), guildId);
    }

    public List<GuildResult.Summary> recent(int limit) {
        return guildService.recent(limit);
    }
}
