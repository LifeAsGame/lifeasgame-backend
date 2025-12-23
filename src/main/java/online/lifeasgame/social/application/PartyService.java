package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.result.PartyResult;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyJoinPolicy;
import online.lifeasgame.social.domain.PartyMemberRole;
import online.lifeasgame.social.domain.PartyVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class PartyService {

    private final PartyReader partyReader;
    private final PartyWriter partyWriter;

    @Transactional
    public PartyResult.Info create(Long playerId, PartyCommand.Create command) {
        Party party = partyWriter.create(
                Party.create(
                        playerId,
                        command.name(),
                        command.code(),
                        command.descriptionMd(),
                        command.bannerImageUrl(),
                        command.bannerBgColor(),
                        command.visibility() == null ? null : PartyVisibility.valueOf(command.visibility()),
                        command.joinPolicy() == null ? null : PartyJoinPolicy.valueOf(command.joinPolicy()),
                        command.maxMembers()
                )
        );
        
        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info rename(Long playerId, Long id, PartyCommand.Rename command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        party.rename(command.name());
        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info changePolicy(Long playerId, Long id, PartyCommand.ChangePolicy command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);

        if (command.visibility() != null) {
            party.changeVisibility(PartyVisibility.valueOf(command.visibility()));
        }
        if (command.joinPolicy() != null) {
            party.changeJoinPolicy(PartyJoinPolicy.valueOf(command.joinPolicy()));
        }
        if (command.maxMembers() > 0) {
            party.changeMaxMembers(command.maxMembers());
        }

        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info changeDescription(Long playerId, Long id, PartyCommand.ChangeDescription command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        party.updateDescription(command.descriptionMd());
        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info changeBanner(Long playerId, Long id, PartyCommand.ChangeEmblem command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        party.updateBanner(command.emblemImageUrl(), command.emblemBgColor());
        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info addTag(Long playerId, Long id, PartyCommand.TagOp command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        party.addTag(command.tag());
        return PartyResult.Info.from(party);
    }

    @Transactional
    public PartyResult.Info removeTag(Long playerId, Long id, PartyCommand.TagOp command) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        party.removeTag(command.tag());
        return PartyResult.Info.from(party);
    }

    // 가입/권한
    @Transactional
    public void requestJoin(Long playerId, Long id, PartyCommand.RequestJoin command) {
        Party party = partyReader.getById(id);
        party.requestJoin(playerId, command.message());
    }

    @Transactional
    public void approveJoin(Long playerId, Long id, PartyCommand.Approve command) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.approveJoin(command.applicantPlayerId());
    }

    @Transactional
    public void rejectJoin(Long playerId, Long id, PartyCommand.Reject command) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.rejectJoin(command.applicantPlayerId());
    }

    @Transactional
    public void cancelJoin(Long playerId, Long id) {
        Party party = partyReader.getById(id);
        party.cancelJoinRequest(playerId);
    }

    @Transactional
    public void transferLeader(Long playerId, Long id, PartyCommand.TransferLeader command) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.transferLeadership(command.fromLeaderPlayerId(), command.toPlayerId());
    }

    @Transactional
    public void kick(Long playerId, Long id, PartyCommand.Kick command) {
        Party party = partyReader.getById(id);
        ensureLeaderOrOfficer(party, playerId);
        party.kickMember(command.targetPlayerId());
    }

    @Transactional
    public void promote(Long playerId, Long id, PartyCommand.Promote command) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.promoteOfficer(party.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void demote(Long playerId, Long id, PartyCommand.Demote command) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.demoteToMember(party.getLeaderPlayerId(), command.targetPlayerId());
    }

    @Transactional
    public void leave(Long playerId, Long id) {
        Party party = partyReader.getById(id);
        party.leave(playerId);
    }

    @Transactional
    public void disbandByLeader(Long playerId, Long id) {
        Party party = partyReader.getById(id);
        ensureLeader(party, playerId);
        party.disbandByLeader(playerId);
    }

    @Transactional
    public void invite(Long playerId, Long id, PartyCommand.Invite command) {
        Party party = partyReader.getById(id);
        ensureLeaderOrOfficer(party, playerId);
        party.invite(playerId, command.inviteePlayerId(), command.message(), parseDateTime(command.expiresAtIso()));
    }

    @Transactional
    public void acceptInvitation(Long playerId, Long id) {
        Party party = partyReader.getById(id);
        party.acceptInvitation(playerId);
    }

    @Transactional
    public void declineInvitation(Long playerId, Long id) {
        Party party = partyReader.getById(id);
        party.declineInvitation(playerId);
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

    public PartyResult.Info getParty(Long playerId, Long id) {
        Party party = partyReader.getByPlayerIdAndId(playerId, id);
        return PartyResult.Info.from(party);
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

    private static LocalDateTime parseDateTime(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(iso);
        } catch (DateTimeParseException e) {
            throw new DomainException(SocialError.INVALID_STATE, "INVALID_EXPIRES_AT");
        }
    }
}
