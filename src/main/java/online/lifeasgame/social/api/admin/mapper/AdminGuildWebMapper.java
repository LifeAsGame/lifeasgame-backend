package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminGuildRequest;
import online.lifeasgame.social.api.admin.response.AdminGuildResponse;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;

import java.util.List;

public final class AdminGuildWebMapper {

    private AdminGuildWebMapper() {
    }

    public static GuildCommand.Create toCreateCommand(AdminGuildRequest.Create request) {
        return new GuildCommand.Create(
                request.name(),
                request.code(),
                request.descriptionMd(),
                request.emblemImageUrl(),
                request.emblemBgColor(),
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static GuildCommand.Rename toRenameCommand(AdminGuildRequest.Rename request) {
        return new GuildCommand.Rename(request.name());
    }

    public static GuildCommand.ChangePolicy toChangePolicyCommand(AdminGuildRequest.ChangePolicy request) {
        return new GuildCommand.ChangePolicy(
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static GuildCommand.ChangeDescription toChangeDescriptionCommand(
            AdminGuildRequest.ChangeDescription request
    ) {
        return new GuildCommand.ChangeDescription(request.descriptionMd());
    }

    public static GuildCommand.ChangeEmblem toChangeEmblemCommand(
            AdminGuildRequest.ChangeEmblem request
    ) {
        return new GuildCommand.ChangeEmblem(request.emblemImageUrl(), request.emblemBgColor());
    }

    public static GuildCommand.TagOp toTagOpCommand(AdminGuildRequest.TagOp request) {
        return new GuildCommand.TagOp(request.tag());
    }

    public static GuildCommand.Approve toApproveCommand(AdminGuildRequest.Approve request) {
        return new GuildCommand.Approve(request.applicantPlayerId());
    }

    public static GuildCommand.Reject toRejectCommand(AdminGuildRequest.Reject request) {
        return new GuildCommand.Reject(request.applicantPlayerId());
    }

    public static GuildCommand.Kick toKickCommand(AdminGuildRequest.Kick request) {
        return new GuildCommand.Kick(request.targetPlayerId());
    }

    public static GuildCommand.TransferLeader toTransferLeaderCommand(AdminGuildRequest.TransferLeader request) {
        return new GuildCommand.TransferLeader(
                request.fromLeaderPlayerId(),
                request.toPlayerId()
        );
    }

    public static GuildCommand.Invite toInviteCommand(AdminGuildRequest.Invite request) {
        return new GuildCommand.Invite(
                request.inviteePlayerId(),
                request.message(),
                request.expiresAt()
        );
    }

    public static GuildCommand.Promote toPromoteCommand(AdminGuildRequest.MemberOp request) {
        return new GuildCommand.Promote(request.targetPlayerId());
    }

    public static GuildCommand.Demote toDemoteCommand(AdminGuildRequest.MemberOp request) {
        return new GuildCommand.Demote(request.targetPlayerId());
    }

    public static GuildCommand.RequestJoin toRequestJoinCommand(AdminGuildRequest.RequestJoin requezt) {
        return new GuildCommand.RequestJoin(requezt.message());
    }

    public static AdminGuildResponse.Summary toSummary(GuildResult.Summary result) {
        return new AdminGuildResponse.Summary(
                result.id(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers()
        );
    }

    public static AdminGuildResponse.Detail toDetail(GuildResult.Info result) {
        return new AdminGuildResponse.Detail(
                result.id(),
                result.playerId(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers(),
                result.tags(),
                result.descriptionMd(),
                result.emblemImageUrl(),
                result.emblemBgColor(),
                result.leaderPlayerId(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static List<AdminGuildResponse.Summary> toSummaries(List<GuildResult.Summary> results) {
        return results.stream()
                .map(AdminGuildWebMapper::toSummary)
                .toList();
    }

    public static AdminGuildResponse.Page<AdminGuildResponse.Summary> toSummaryPage(
            GuildResult.Page<GuildResult.Summary> result
    ) {
        return new AdminGuildResponse.Page<>(
                toSummaries(result.contents()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static AdminGuildResponse.Info toInfo(GuildResult.Info result) {
        return new AdminGuildResponse.Info(
                result.id(),
                result.playerId(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers(),
                result.tags(),
                result.descriptionMd(),
                result.emblemImageUrl(),
                result.emblemBgColor(),
                result.leaderPlayerId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
