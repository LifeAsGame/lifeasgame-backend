package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.model.PartySpec;
import online.lifeasgame.social.application.result.PartyResult;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyMemberRole;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PartyService {

    private final PartyReader partyReader;
    private final PartyWriter partyWriter;

    @Transactional
    public PartyResult.Info create(Long playerId, PartyCommand.Create c) {
        Party saved = partyWriter.create(PartySpec.Create.from(playerId, c));
        return PartyResult.Info.from(saved);
    }

    @Transactional
    public PartyResult.Info rename(Long playerId, Long id, PartyCommand.Rename c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.rename(party, c));
    }

    @Transactional
    public PartyResult.Info changePolicy(Long playerId, Long id, PartyCommand.ChangePolicy c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.changePolicy(party, c));
    }

    @Transactional
    public PartyResult.Info changeDescription(Long playerId, Long id, PartyCommand.ChangeDescription c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.changeDescription(party, c));
    }

    @Transactional
    public PartyResult.Info changeEmblem(Long playerId, Long id, PartyCommand.ChangeEmblem c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.changeBanner(party, c));
    }

    @Transactional
    public PartyResult.Info addTag(Long playerId, Long id, PartyCommand.TagOp c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.addTag(party, c));
    }

    @Transactional
    public PartyResult.Info removeTag(Long playerId, Long id, PartyCommand.TagOp c) {
        Party party = partyReader.getOwned(playerId, id);
        return PartyResult.Info.from(partyWriter.removeTag(party, c));
    }

    // 가입/권한
    @Transactional
    public void requestJoin(Long playerId, Long id, PartyCommand.RequestJoin c) {
        Party party = partyReader.get(id);
        partyWriter.requestJoin(party, playerId, c);
    }

    @Transactional
    public void approveJoin(Long playerId, Long id, PartyCommand.Approve c) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.approveJoin(party, c);
    }

    @Transactional
    public void rejectJoin(Long playerId, Long id, PartyCommand.Reject c) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.rejectJoin(party, c);
    }

    @Transactional
    public void cancelJoin(Long playerId, Long id) {
        Party party = partyReader.get(id);
        partyWriter.cancelJoin(party, playerId);
    }

    @Transactional
    public void transferLeader(Long playerId, Long id, PartyCommand.TransferLeader c) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.transferLeader(party, c);
    }

    @Transactional
    public void kick(Long playerId, Long id, PartyCommand.Kick c) {
        Party party = partyReader.get(id);
        ensureLeaderOrOfficer(party, playerId);
        partyWriter.kick(party, c);
    }

    @Transactional
    public void promote(Long playerId, Long id, PartyCommand.Promote c) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.promote(party, c);
    }

    @Transactional
    public void demote(Long playerId, Long id, PartyCommand.Demote c) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.demote(party, c);
    }

    @Transactional
    public void leave(Long playerId, Long id) {
        Party party = partyReader.get(id);
        partyWriter.leave(party, playerId);
    }

    @Transactional
    public void disbandByLeader(Long playerId, Long id) {
        Party party = partyReader.get(id);
        ensureLeader(party, playerId);
        partyWriter.disbandByLeader(party, playerId);
    }

    // 초대
    @Transactional
    public void invite(Long playerId, Long id, PartyCommand.Invite c) {
        Party party = partyReader.get(id);
        ensureLeaderOrOfficer(party, playerId);
        partyWriter.invite(party, playerId, c);
    }

    @Transactional
    public void acceptInvitation(Long playerId, Long id) {
        Party party = partyReader.get(id);
        partyWriter.acceptInvitation(party, playerId);
    }

    @Transactional
    public void declineInvitation(Long playerId, Long id) {
        Party party = partyReader.get(id);
        partyWriter.declineInvitation(party, playerId);
    }

    public PartyResult.Page<PartyResult.Summary> search(String keyword, String visibility, int page, int size) {
        List<Party> domains = partyReader.search(keyword, visibility, page, size);
        long total = partyReader.countSearch(keyword, visibility);
        List<PartyResult.Summary> contents = domains.stream().map(PartyResult.Summary::from).toList();
        return PartyResult.Page.of(contents, page, size, total);
    }

    public List<PartyResult.Summary> recent(int limit) {
        return partyReader.recent(limit).stream().map(PartyResult.Summary::from).toList();
    }

    public PartyResult.Summary getParty(Long playerId, Long id) {
        Party party = partyReader.getParty(playerId, id);
        return PartyResult.Summary.from(party);
    }

    private static void ensureLeader(Party party, Long actorId) {
        var me = party.findMember(actorId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() != PartyMemberRole.LEADER) {
            throw new DomainException(SocialError.LEADER_ONLY);
        }
    }

    private static void ensureLeaderOrOfficer(Party party, Long actorId) {
        var me = party.findMember(actorId).orElseThrow(() -> new DomainException(SocialError.NOT_MEMBER));
        if (me.getRole() == PartyMemberRole.MEMBER) {
            throw new DomainException(SocialError.OFFICER_OR_LEADER_ONLY);
        }
    }
}
