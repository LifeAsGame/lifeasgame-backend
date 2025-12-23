package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminPartyRequest;
import online.lifeasgame.social.api.admin.response.AdminPartyResponse;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;

import java.util.List;

public final class AdminPartyWebMapper {

    private AdminPartyWebMapper() {
    }

    public static PartyCommand.Create toCreateCommand(AdminPartyRequest.Create request) {
        return new PartyCommand.Create(
                request.name(),
                request.code(),
                request.descriptionMd(),
                request.bannerImageUrl(),
                request.bannerBgColor(),
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static PartyCommand.Rename toRenameCommand(AdminPartyRequest.Rename request) {
        return new PartyCommand.Rename(request.name());
    }

    public static PartyCommand.ChangePolicy toChangePolicyCommand(AdminPartyRequest.ChangePolicy request) {
        return new PartyCommand.ChangePolicy(
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static PartyCommand.ChangeDescription toChangeDescriptionCommand(AdminPartyRequest.ChangeDescription request) {
        return new PartyCommand.ChangeDescription(request.descriptionMd());
    }

    public static PartyCommand.ChangeEmblem toChangeEmblemCommand(AdminPartyRequest.ChangeEmblem request) {
        return new PartyCommand.ChangeEmblem(
                request.emblemImageUrl(),
                request.emblemBgColor()
        );
    }

    public static PartyCommand.TagOp toTagOpCommand(AdminPartyRequest.TagOp request) {
        return new PartyCommand.TagOp(request.tag());
    }

    public static PartyCommand.Approve toApproveCommand(AdminPartyRequest.Approve request) {
        return new PartyCommand.Approve(request.applicantPlayerId());
    }

    public static PartyCommand.Reject toRejectCommand(AdminPartyRequest.Reject request) {
        return new PartyCommand.Reject(request.applicantPlayerId());
    }

    public static PartyCommand.Kick toKickCommand(AdminPartyRequest.Kick request) {
        return new PartyCommand.Kick(request.targetPlayerId());
    }

    public static PartyCommand.TransferLeader toTransferLeaderCommand(AdminPartyRequest.TransferLeader request) {
        return new PartyCommand.TransferLeader(
                request.fromLeaderPlayerId(),
                request.toPlayerId()
        );
    }

    public static PartyCommand.Invite toInviteCommand(AdminPartyRequest.Invite request) {
        return new PartyCommand.Invite(
                request.inviteePlayerId(),
                request.message(),
                request.expiresAt()
        );
    }

    public static PartyCommand.Promote toPromoteCommand(AdminPartyRequest.MemberOp request) {
        return new PartyCommand.Promote(request.targetPlayerId());
    }

    public static PartyCommand.Demote toDemoteCommand(AdminPartyRequest.MemberOp request) {
        return new PartyCommand.Demote(request.targetPlayerId());
    }

    public static PartyCommand.RequestJoin toRequestJoinCommand(AdminPartyRequest.RequestJoin request) {
        return new PartyCommand.RequestJoin(request.message());
    }

    public static AdminPartyResponse.Summary toSummary(PartyResult.Summary result) {
        return new AdminPartyResponse.Summary(
                result.id(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers()
        );
    }

    public static AdminPartyResponse.Detail toDetail(PartyResult.Info result) {
        return new AdminPartyResponse.Detail(
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
                result.bannerImageUrl(),
                result.bannerBgColor(),
                result.leaderPlayerId(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static List<AdminPartyResponse.Summary> toSummaries(List<PartyResult.Summary> results) {
        return results.stream()
                .map(AdminPartyWebMapper::toSummary)
                .toList();
    }

    public static AdminPartyResponse.Info toInfo(PartyResult.Info result) {
        return new AdminPartyResponse.Info(
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
                result.bannerImageUrl(),
                result.bannerBgColor(),
                result.leaderPlayerId(),
                result.createdAt(),
                result.updatedAt()
        );
    }


    public static AdminPartyResponse.Page<AdminPartyResponse.Summary> toSummaryPage(
            PartyResult.Page<PartyResult.Summary> result
    ) {
        return new AdminPartyResponse.Page<>(
                toSummaries(result.contents()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
