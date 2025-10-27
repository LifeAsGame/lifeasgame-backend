package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminGuildRequest;
import online.lifeasgame.social.api.admin.response.AdminGuildResponse;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;

public final class AdminGuildWebMapper {
    private AdminGuildWebMapper() {
    }

    // Request → Command
    public static GuildCommand.Create toCommand(AdminGuildRequest.Create r) {
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

    public static GuildCommand.Rename toCommand(AdminGuildRequest.Rename r) {
        return GuildCommand.Rename.of(r.name());
    }

    public static GuildCommand.ChangePolicy toCommand(AdminGuildRequest.ChangePolicy r) {
        return GuildCommand.ChangePolicy.of(r.visibility(), r.joinPolicy(), r.maxMembers());
    }

    public static GuildCommand.ChangeDescription toCommand(AdminGuildRequest.ChangeDescription r) {
        return GuildCommand.ChangeDescription.of(r.descriptionMd());
    }

    public static GuildCommand.ChangeEmblem toCommand(AdminGuildRequest.ChangeEmblem r) {
        return GuildCommand.ChangeEmblem.of(r.emblemImageUrl(), r.emblemBgColor());
    }

    public static GuildCommand.TagOp toCommand(AdminGuildRequest.TagOp r) {
        return GuildCommand.TagOp.of(r.tag());
    }

    public static GuildCommand.Approve toCommand(AdminGuildRequest.Approve r) {
        return GuildCommand.Approve.of(r.applicantPlayerId());
    }

    public static GuildCommand.Reject toCommand(AdminGuildRequest.Reject r) {
        return GuildCommand.Reject.of(r.applicantPlayerId());
    }

    public static GuildCommand.Kick toCommand(AdminGuildRequest.Kick r) {
        return GuildCommand.Kick.of(r.targetPlayerId());
    }

    public static GuildCommand.TransferLeader toCommand(AdminGuildRequest.TransferLeader r) {
        return GuildCommand.TransferLeader.of(r.fromLeaderPlayerId(), r.toPlayerId());
    }

    public static GuildCommand.Invite toCommand(AdminGuildRequest.Invite r) {
        return GuildCommand.Invite.of(r.inviteePlayerId(), r.message(), r.expiresAt());
    }

    public static GuildCommand.Promote toCommandPromote(AdminGuildRequest.MemberOp r) {
        return GuildCommand.Promote.of(r.targetPlayerId());
    }

    public static GuildCommand.Demote toCommandDemote(AdminGuildRequest.MemberOp r) {
        return GuildCommand.Demote.of(r.targetPlayerId());
    }

    public static GuildCommand.RequestJoin toCommand(AdminGuildRequest.RequestJoin r) {
        return GuildCommand.RequestJoin.of(r.message());
    }

    // Result → Response
    public static AdminGuildResponse.Summary toSummary(GuildResult.Summary r) {
        return AdminGuildResponse.Summary.of(
                r.id(),
                r.name(),
                r.code(),
                r.visibility(),
                r.joinPolicy(),
                r.status(),
                r.maxMembers()
        );
    }

    public static AdminGuildResponse.Detail toDetail(GuildResult.Info r) {
        return AdminGuildResponse.Detail.of(
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

    public static java.util.List<AdminGuildResponse.Summary> toSummaries(java.util.List<GuildResult.Summary> rs) {
        return rs.stream().map(AdminGuildWebMapper::toSummary).toList();
    }

    public static AdminGuildResponse.Page<AdminGuildResponse.Summary> toSummaryPage(GuildResult.Page<GuildResult.Summary> p) {
        return AdminGuildResponse.Page.of(
                toSummaries(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }

    public static AdminGuildResponse.Info toInfo(GuildResult.Info r) {
        return AdminGuildResponse.Info.of(
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
}
