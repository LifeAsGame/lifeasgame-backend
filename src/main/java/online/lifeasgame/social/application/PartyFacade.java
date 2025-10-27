package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.security.CurrentPlayerAccessor;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class PartyFacade {

    private final PartyService partyService;
    private final CurrentPlayerAccessor currentPlayerAccessor;

    private Long player() {
        return currentPlayerAccessor.currentPlayerIdOrThrow();
    }

    public PartyResult.Info create(PartyCommand.Create c) {
        return partyService.create(player(), c);
    }

    public PartyResult.Info rename(Long partyId, PartyCommand.Rename c) {
        return partyService.rename(player(), partyId, c);
    }

    public PartyResult.Info changePolicy(Long partyId, PartyCommand.ChangePolicy c) {
        return partyService.changePolicy(player(), partyId, c);
    }

    public PartyResult.Info changeDescription(Long partyId, PartyCommand.ChangeDescription c) {
        return partyService.changeDescription(player(), partyId, c);
    }

    public PartyResult.Info changeBanner(Long partyId, PartyCommand.ChangeEmblem c) {
        return partyService.changeBanner(player(), partyId, c);
    }

    public PartyResult.Info addTag(Long partyId, PartyCommand.TagOp c) {
        return partyService.addTag(player(), partyId, c);
    }

    public PartyResult.Info removeTag(Long partyId, PartyCommand.TagOp c) {
        return partyService.removeTag(player(), partyId, c);
    }

    public void requestJoin(Long partyId, PartyCommand.RequestJoin c) {
        partyService.requestJoin(player(), partyId, c);
    }

    public void approveJoin(Long partyId, PartyCommand.Approve c) {
        partyService.approveJoin(player(), partyId, c);
    }

    public void rejectJoin(Long partyId, PartyCommand.Reject c) {
        partyService.rejectJoin(player(), partyId, c);
    }

    public void cancelJoin(Long partyId) {
        partyService.cancelJoin(player(), partyId);
    }

    public void transferLeader(Long partyId, PartyCommand.TransferLeader c) {
        partyService.transferLeader(player(), partyId, c);
    }

    public void kick(Long partyId, PartyCommand.Kick c) {
        partyService.kick(player(), partyId, c);
    }

    public void promote(Long partyId, PartyCommand.Promote c) {
        partyService.promote(player(), partyId, c);
    }

    public void demote(Long partyId, PartyCommand.Demote c) {
        partyService.demote(player(), partyId, c);
    }

    public void leave(Long partyId) {
        partyService.leave(player(), partyId);
    }

    public void disband(Long partyId) {
        partyService.disbandByLeader(player(), partyId);
    }

    public void invite(Long partyId, PartyCommand.Invite c) {
        partyService.invite(player(), partyId, c);
    }

    public void acceptInvitation(Long partyId) {
        partyService.acceptInvitation(player(), partyId);
    }

    public void declineInvitation(Long partyId) {
        partyService.declineInvitation(player(), partyId);
    }

    public PartyResult.Page<PartyResult.Summary> search(String keyword, String visibility, int page, int size) {
        return partyService.search(keyword, visibility, page, size);
    }

    public PartyResult.Info getParty(Long partyId) {
        return partyService.getParty(player(), partyId);
    }

    public List<PartyResult.Summary> recent(int limit) {
        return partyService.recent(limit);
    }
}
