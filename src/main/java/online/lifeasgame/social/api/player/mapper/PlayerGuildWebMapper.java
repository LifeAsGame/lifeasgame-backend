package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerGuildRequest;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;

import java.util.List;

public final class PlayerGuildWebMapper {
    private PlayerGuildWebMapper() {
    }

    // Request → Command
    public static GuildCommand.Create toCommand(PlayerGuildRequest.Create r) {
        return GuildCommand.Create.of(
                r.name(),
                r.code(),
                r.descriptionMd(),
                r.emblemImageUrl(),
                r.emblemBgColor(),
                r.visibility(),
                r.joinPolicy(),
                r.maxMembers()
        );
    }

    public static GuildCommand.Rename toCommand(PlayerGuildRequest.Rename r) {
        return GuildCommand.Rename.of(r.name());
    }

    public static GuildCommand.ChangePolicy toCommand(PlayerGuildRequest.ChangePolicy r) {
        return GuildCommand.ChangePolicy.of(r.visibility(), r.joinPolicy(), r.maxMembers());
    }

    public static GuildCommand.ChangeDescription toCommand(PlayerGuildRequest.ChangeDescription r) {
        return GuildCommand.ChangeDescription.of(r.descriptionMd());
    }

    public static GuildCommand.ChangeEmblem toCommand(PlayerGuildRequest.ChangeEmblem r) {
        return GuildCommand.ChangeEmblem.of(r.emblemImageUrl(), r.emblemBgColor());
    }

    public static GuildCommand.TagOp toCommand(PlayerGuildRequest.TagOp r) {
        return GuildCommand.TagOp.of(r.tag());
    }

    public static GuildCommand.RequestJoin toCommand(PlayerGuildRequest.RequestJoin r) {
        return GuildCommand.RequestJoin.of(r.message());
    }

    public static GuildCommand.Approve toCommand(PlayerGuildRequest.Approve r) {
        return GuildCommand.Approve.of(r.applicantPlayerId());
    }

    public static GuildCommand.Reject toCommand(PlayerGuildRequest.Reject r) {
        return GuildCommand.Reject.of(r.applicantPlayerId());
    }

    public static GuildCommand.TransferLeader toCommand(PlayerGuildRequest.TransferLeader r) {
        return GuildCommand.TransferLeader.of(r.fromLeaderPlayerId(), r.toPlayerId());
    }

    public static GuildCommand.Kick toCommand(PlayerGuildRequest.Kick r) {
        return GuildCommand.Kick.of(r.targetPlayerId());
    }

    public static GuildCommand.Promote toCommandPromote(PlayerGuildRequest.MemberOp r) {
        return GuildCommand.Promote.of(r.targetPlayerId());
    }

    public static GuildCommand.Demote toCommandDemote(PlayerGuildRequest.MemberOp r) {
        return GuildCommand.Demote.of(r.targetPlayerId());
    }

    public static GuildCommand.Invite toCommand(PlayerGuildRequest.Invite r) {
        return GuildCommand.Invite.of(r.inviteePlayerId(), r.message(), r.expiresAt());
    }

    // Result → Response
    public static PlayerGuildResponse.Summary toSummary(GuildResult.Summary r) {
        return PlayerGuildResponse.Summary.of(
                r.id(),
                r.name(),
                r.code(),
                r.visibility(),
                r.joinPolicy(),
                r.status(),
                r.maxMembers()
        );
    }

    public static PlayerGuildResponse.Info toInfo(GuildResult.Info r) {
        return PlayerGuildResponse.Info.of(
                r.id(),
                r.playerId(),
                r.name(),
                r.code(),
                r.visibility(),
                r.joinPolicy(),
                r.status(),
                r.maxMembers(),
                r.tags(),
                r.descriptionMd(),
                r.emblemImageUrl(),
                r.emblemBgColor(),
                r.leaderPlayerId(),
                r.createdAt(),
                r.updatedAt()
        );
    }

    public static List<PlayerGuildResponse.Summary> toSummaryList(List<GuildResult.Summary> rs) {
        return rs.stream().map(PlayerGuildWebMapper::toSummary).toList();
    }

    public static PlayerGuildResponse.Page<PlayerGuildResponse.Summary> toSummaryPage(GuildResult.Page<GuildResult.Summary> p) {
        return PlayerGuildResponse.Page.of(
                toSummaryList(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
