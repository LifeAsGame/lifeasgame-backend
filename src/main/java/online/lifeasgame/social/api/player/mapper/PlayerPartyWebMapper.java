package online.lifeasgame.social.api.player.mapper;

import online.lifeasgame.social.api.player.request.PlayerPartyRequest;
import online.lifeasgame.social.api.player.response.PlayerPartyResponse;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;

import java.util.List;

public final class PlayerPartyWebMapper {

    private PlayerPartyWebMapper() {
    }

    public static List<PlayerPartyResponse.Summary> toSummaries(List<PartyResult.Summary> results) {
        return results.stream()
                .map(PlayerPartyWebMapper::toSummary)
                .toList();
    }

    public static PlayerPartyResponse.Summary toSummary(PartyResult.Summary result) {
        return new PlayerPartyResponse.Summary(
                result.id(),
                result.name(),
                result.code(),
                result.visibility(),
                result.joinPolicy(),
                result.status(),
                result.maxMembers()
        );
    }

    public static PlayerPartyResponse.Info toInfo(PartyResult.Info result) {
        return new PlayerPartyResponse.Info(
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

    public static PlayerPartyResponse.Page<PlayerPartyResponse.Summary> toSummaryPage(
            PartyResult.Page<PartyResult.Summary> result
    ) {
        return new PlayerPartyResponse.Page<>(
                toSummaries(result.contents()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    public static PlayerPartyResponse.MyParty toMy(PartyResult.MyParty result) {
        return new PlayerPartyResponse.MyParty(result.id(), result.name(), result.code(), result.status(),
                result.myRole(), result.memberCount(), result.maxMembers());
    }

    public static PlayerPartyResponse.Member toMember(PartyResult.Member result) {
        return new PlayerPartyResponse.Member(result.playerId(), result.role(), result.joinedAt());
    }

    public static PlayerPartyResponse.WaitMember toPending(PartyResult.Pending result) {
        return new PlayerPartyResponse.WaitMember(result.id(), result.partyId(), result.name(), result.code(),
                result.playerId(), result.type(), result.status(), result.message(), result.requestedAt(), result.expiresAt());
    }

    public static PlayerPartyResponse.Me toMe(PartyResult.Me result) {
        return new PlayerPartyResponse.Me(result.myRole(), result.pendingJoin(), result.pendingInvitation(), result.actions());
    }

    public static <T, U> PlayerPartyResponse.Page<U> toPage(PartyResult.Page<T> page, java.util.function.Function<T, U> mapper) {
        return new PlayerPartyResponse.Page<>(page.contents().stream().map(mapper).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    public static PartyCommand.Create toCreateCommand(PlayerPartyRequest.Create request) {
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

    public static PartyCommand.Rename toRenameCommand(PlayerPartyRequest.Rename request) {
        return new PartyCommand.Rename(request.name());
    }

    public static PartyCommand.ChangePolicy toChangePolicyCommand(PlayerPartyRequest.ChangePolicy request) {
        return new PartyCommand.ChangePolicy(
                request.visibility(),
                request.joinPolicy(),
                request.maxMembers()
        );
    }

    public static PartyCommand.ChangeDescription toChangeDescriptionCommand(
            PlayerPartyRequest.ChangeDescription request
    ) {
        return new PartyCommand.ChangeDescription(request.descriptionMd());
    }

    public static PartyCommand.ChangeEmblem toChangeEmblemCommand(PlayerPartyRequest.ChangeBanner request) {
        return new PartyCommand.ChangeEmblem(
                request.bannerImageUrl(),
                request.bannerBgColor()
        );
    }

    public static PartyCommand.TagOp toTagOpCommand(PlayerPartyRequest.TagOp request) {
        return new PartyCommand.TagOp(request.tag());
    }

    public static PartyCommand.RequestJoin toRequestJoinCommand(PlayerPartyRequest.RequestJoin request) {
        return new PartyCommand.RequestJoin(request.message());
    }

    public static PartyCommand.Approve toApproveCommand(PlayerPartyRequest.Approve request) {
        return new PartyCommand.Approve(request.applicantPlayerId());
    }

    public static PartyCommand.Reject toRejectCommand(PlayerPartyRequest.Reject request) {
        return new PartyCommand.Reject(request.applicantPlayerId());
    }

    public static PartyCommand.Invite toInviteCommand(PlayerPartyRequest.Invite request) {
        return new PartyCommand.Invite(
                request.inviteePlayerId(),
                request.message(),
                request.expiresAt()
        );
    }

    public static PartyCommand.TransferLeader toTransferLeaderCommand(PlayerPartyRequest.TransferLeader request) {
        return new PartyCommand.TransferLeader(
                request.fromLeaderPlayerId(),
                request.toPlayerId()
        );
    }

    public static PartyCommand.Kick toKickCommand(PlayerPartyRequest.Kick request) {
        return new PartyCommand.Kick(request.targetPlayerId());
    }

    public static PartyCommand.Promote toPromoteCommand(PlayerPartyRequest.MemberOp request) {
        return new PartyCommand.Promote(request.targetPlayerId());
    }

    public static PartyCommand.Demote toDemoteCommand(PlayerPartyRequest.MemberOp request) {
        return new PartyCommand.Demote(request.targetPlayerId());
    }
}
