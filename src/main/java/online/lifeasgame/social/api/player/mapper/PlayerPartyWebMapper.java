package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;

import java.util.List;

public final class PlayerPartyWebMapper {
    private PlayerPartyWebMapper() {
    }

    // Request → Command
    public static PartyCommand.Create toCommand(PlayerPartyRequest.Create r) {
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

    public static PartyCommand.Rename toCommand(PlayerPartyRequest.Rename r) {
        return PartyCommand.Rename.of(r.name());
    }

    public static PartyCommand.ChangePolicy toCommand(PlayerPartyRequest.ChangePolicy r) {
        return PartyCommand.ChangePolicy.of(r.visibility(), r.joinPolicy(), r.maxMembers());
    }

    public static PartyCommand.ChangeDescription toCommand(PlayerPartyRequest.ChangeDescription r) {
        return PartyCommand.ChangeDescription.of(r.descriptionMd());
    }

    public static PartyCommand.ChangeEmblem toCommand(PlayerPartyRequest.ChangeBanner r) {
        return PartyCommand.ChangeEmblem.of(r.bannerImageUrl(), r.bannerBgColor());
    }

    public static PartyCommand.TagOp toCommand(PlayerPartyRequest.TagOp r) {
        return PartyCommand.TagOp.of(r.tag());
    }

    public static PartyCommand.RequestJoin toCommand(PlayerPartyRequest.RequestJoin r) {
        return PartyCommand.RequestJoin.of(r.message());
    }

    public static PartyCommand.Approve toCommand(PlayerPartyRequest.Approve r) {
        return PartyCommand.Approve.of(r.applicantPlayerId());
    }

    public static PartyCommand.Reject toCommand(PlayerPartyRequest.Reject r) {
        return PartyCommand.Reject.of(r.applicantPlayerId());
    }

    public static PartyCommand.TransferLeader toCommand(PlayerPartyRequest.TransferLeader r) {
        return PartyCommand.TransferLeader.of(r.fromLeaderPlayerId(), r.toPlayerId());
    }

    public static PartyCommand.Kick toCommand(PlayerPartyRequest.Kick r) {
        return PartyCommand.Kick.of(r.targetPlayerId());
    }

    public static PartyCommand.Promote toCommandPromote(PlayerPartyRequest.MemberOp r) {
        return PartyCommand.Promote.of(r.targetPlayerId());
    }

    public static PartyCommand.Demote toCommandDemote(PlayerPartyRequest.MemberOp r) {
        return PartyCommand.Demote.of(r.targetPlayerId());
    }

    public static PartyCommand.Invite toCommand(PlayerPartyRequest.Invite r) {
        return PartyCommand.Invite.of(r.inviteePlayerId(), r.message(), r.expiresAt());
    }

    // Result → Response
    public static PlayerPartyResponse.Summary toSummary(PartyResult.Summary r) {
        return PlayerPartyResponse.Summary.of(
                r.id(),
                r.name(),
                r.code(),
                r.visibility(),
                r.joinPolicy(),
                r.status(),
                r.maxMembers()
        );
    }

    public static PlayerPartyResponse.Info toInfo(PartyResult.Info r) {
        return PlayerPartyResponse.Info.of(
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

    public static List<PlayerPartyResponse.Summary> toSummaryList(List<PartyResult.Summary> rs) {
        return rs.stream().map(PlayerPartyWebMapper::toSummary).toList();
    }

    public static PlayerPartyResponse.Page<PlayerPartyResponse.Summary> toSummaryPage(PartyResult.Page<PartyResult.Summary> p) {
        return PlayerPartyResponse.Page.of(
                toSummaryList(p.contents()),
                p.page(),
                p.size(),
                p.totalElements(),
                p.totalPages()
        );
    }
}
