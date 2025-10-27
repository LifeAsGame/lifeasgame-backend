package online.lifeasgame.social.api.admin.mapper;

import online.lifeasgame.social.api.admin.request.AdminPartyRequest;
import online.lifeasgame.social.api.admin.response.AdminPartyResponse;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;

import java.util.List;

public final class AdminPartyWebMapper {
    private AdminPartyWebMapper() {
    }

    // Request → Command
    public static PartyCommand.Create toCommand(AdminPartyRequest.Create r) {
        return PartyCommand.Create.of(
                r.name(),
                r.code(),
                r.descriptionMd(),
                r.bannerImageUrl(),
                r.bannerBgColor(),
                r.visibility(),
                r.joinPolicy(),
                r.maxMembers()
        );
    }

    public static PartyCommand.Rename toCommand(AdminPartyRequest.Rename r) {
        return PartyCommand.Rename.of(r.name());
    }

    public static PartyCommand.ChangePolicy toCommand(AdminPartyRequest.ChangePolicy r) {
        return PartyCommand.ChangePolicy.of(r.visibility(), r.joinPolicy(), r.maxMembers());
    }

    public static PartyCommand.ChangeDescription toCommand(AdminPartyRequest.ChangeDescription r) {
        return PartyCommand.ChangeDescription.of(r.descriptionMd());
    }

    public static PartyCommand.ChangeEmblem toCommand(AdminPartyRequest.ChangeEmblem r) {
        return PartyCommand.ChangeEmblem.of(r.emblemImageUrl(), r.emblemBgColor());
    }

    public static PartyCommand.TagOp toCommand(AdminPartyRequest.TagOp r) {
        return PartyCommand.TagOp.of(r.tag());
    }

    public static PartyCommand.Approve toCommand(AdminPartyRequest.Approve r) {
        return PartyCommand.Approve.of(r.applicantPlayerId());
    }

    public static PartyCommand.Reject toCommand(AdminPartyRequest.Reject r) {
        return PartyCommand.Reject.of(r.applicantPlayerId());
    }

    public static PartyCommand.Kick toCommand(AdminPartyRequest.Kick r) {
        return PartyCommand.Kick.of(r.targetPlayerId());
    }

    public static PartyCommand.TransferLeader toCommand(AdminPartyRequest.TransferLeader r) {
        return PartyCommand.TransferLeader.of(r.fromLeaderPlayerId(), r.toPlayerId());
    }

    public static PartyCommand.Invite toCommand(AdminPartyRequest.Invite r) {
        return PartyCommand.Invite.of(r.inviteePlayerId(), r.message(), r.expiresAt());
    }

    public static PartyCommand.Promote toCommandPromote(AdminPartyRequest.MemberOp r) {
        return PartyCommand.Promote.of(r.targetPlayerId());
    }

    public static PartyCommand.Demote toCommandDemote(AdminPartyRequest.MemberOp r) {
        return PartyCommand.Demote.of(r.targetPlayerId());
    }

    public static PartyCommand.RequestJoin toCommand(AdminPartyRequest.RequestJoin r) {
        return PartyCommand.RequestJoin.of(r.message());
    }

    // Result → Response
    public static AdminPartyResponse.Summary toSummary(PartyResult.Summary r) {
        return AdminPartyResponse.Summary.of(
                r.id(),
                r.name(),
                r.code(),
                r.visibility(),
                r.joinPolicy(),
                r.status(),
                r.maxMembers()
        );
    }

    public static AdminPartyResponse.Detail toDetail(PartyResult.Info r) {
        return AdminPartyResponse.Detail.of(
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
                r.bannerImageUrl(),
                r.bannerBgColor(),
                r.leaderPlayerId(),
                r.createdAt(),
                r.updatedAt()
        );
    }

    public static java.util.List<AdminPartyResponse.Summary> toSummaries(List<PartyResult.Summary> rs) {
        return rs.stream().map(AdminPartyWebMapper::toSummary).toList();
    }

    public static AdminPartyResponse.Info toInfo(PartyResult.Info r) {
        return AdminPartyResponse.Info.of(
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
                r.bannerImageUrl(),
                r.bannerBgColor(),
                r.leaderPlayerId(),
                r.createdAt(),
                r.updatedAt()
        );
    }


    public static AdminPartyResponse.Page<AdminPartyResponse.Summary> toSummaryPage(PartyResult.Page<PartyResult.Summary> p) {
        return AdminPartyResponse.Page.of(
                toSummaries(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
