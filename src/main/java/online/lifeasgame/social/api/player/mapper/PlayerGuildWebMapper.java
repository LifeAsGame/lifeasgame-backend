package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerGuildRequest;
import online.lifeasgame.social.api.player.response.PlayerGuildResponse;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.result.GuildResult;

import java.util.List;

public final class PlayerGuildWebMapper {

    private PlayerGuildWebMapper() {
    }

    public static List<PlayerGuildResponse.Summary> toSummaries(List<GuildResult.Summary> results) {
        return results.stream().map(PlayerGuildWebMapper::toSummary).toList();
    }

    public static PlayerGuildResponse.Summary toSummary(GuildResult.Summary result) {
        return new PlayerGuildResponse.Summary(
                result.id(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers()
        );
    }

    public static PlayerGuildResponse.Info toInfo(GuildResult.Info result) {
        return new PlayerGuildResponse.Info(
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

    public static PlayerGuildResponse.Page<PlayerGuildResponse.Summary> toSummaryPage(GuildResult.Page<GuildResult.Summary> result) {
        return new PlayerGuildResponse.Page<>(
                toSummaries(result.contents()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static GuildCommand.Create toCreateCommand(PlayerGuildRequest.Create request) {
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

    public static GuildCommand.Rename toRenameCommand(PlayerGuildRequest.Rename request) {
        return new GuildCommand.Rename(request.name());
    }

    public static GuildCommand.ChangePolicy toChangePolicyCommand(PlayerGuildRequest.ChangePolicy request) {
        return new GuildCommand.ChangePolicy(
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static GuildCommand.ChangeDescription toChangeDescriptionCommand(PlayerGuildRequest.ChangeDescription request) {
        return new GuildCommand.ChangeDescription(request.descriptionMd());
    }

    public static GuildCommand.ChangeEmblem toChangeEmblemCommand(PlayerGuildRequest.ChangeEmblem request) {
        return new GuildCommand.ChangeEmblem(request.emblemImageUrl(), request.emblemBgColor());
    }

    public static GuildCommand.TagOp toTagOpCommand(PlayerGuildRequest.TagOp request) {
        return new GuildCommand.TagOp(request.tag());
    }

    public static GuildCommand.RequestJoin toRequestJoinCommand(PlayerGuildRequest.RequestJoin request) {
        return new GuildCommand.RequestJoin(request.message());
    }

    public static GuildCommand.Approve toApproveCommand(PlayerGuildRequest.Approve request) {
        return new GuildCommand.Approve(request.applicantPlayerId());
    }

    public static GuildCommand.Reject toRejectCommand(PlayerGuildRequest.Reject request) {
        return new GuildCommand.Reject(request.applicantPlayerId());
    }

    public static GuildCommand.Invite toInviteCommand(PlayerGuildRequest.Invite request) {
        return new GuildCommand.Invite(request.inviteePlayerId(), request.message(), request.expiresAt());
    }

    public static GuildCommand.TransferLeader toTransferLeaderCommand(PlayerGuildRequest.TransferLeader request) {
        return new GuildCommand.TransferLeader(request.toPlayerId());
    }

    public static GuildCommand.Kick toKickCommand(PlayerGuildRequest.Kick request) {
        return new GuildCommand.Kick(request.targetPlayerId());
    }

    public static GuildCommand.Promote toPromoteCommand(PlayerGuildRequest.MemberOp request) {
        return new GuildCommand.Promote(request.targetPlayerId());
    }

    public static GuildCommand.Demote toDemoteCommand(PlayerGuildRequest.MemberOp request) {
        return new GuildCommand.Demote(request.targetPlayerId());
    }
}
