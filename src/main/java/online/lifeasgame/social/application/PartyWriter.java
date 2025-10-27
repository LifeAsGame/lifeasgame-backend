package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.application.model.PartySpec;
import online.lifeasgame.social.domain.Party;
import online.lifeasgame.social.domain.PartyJoinPolicy;
import online.lifeasgame.social.domain.PartyVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.PartyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PartyWriter {

    private final PartyRepository partyRepository;

    public Party create(PartySpec.Create spec) {
        return partyRepository.save(Party.create(
                spec.playerId(),
                spec.name(),
                spec.code(),
                spec.descriptionMd(),
                spec.bannerImageUrl(),
                spec.bannerBgColor(),
                spec.visibility(),
                spec.joinPolicy(),
                spec.maxMembers()
        ));
    }

    public Party rename(Party party, PartyCommand.Rename c) {
        party.rename(c.name());
        return party;
    }

    public Party changePolicy(Party party, PartyCommand.ChangePolicy c) {
        if (c.visibility() != null) {
            party.changeVisibility(PartyVisibility.valueOf(c.visibility()));
        }
        if (c.joinPolicy() != null) {
            party.changeJoinPolicy(PartyJoinPolicy.valueOf(c.joinPolicy()));
        }
        if (c.maxMembers() > 0) {
            party.changeMaxMembers(c.maxMembers());
        }
        return party;
    }

    public Party changeDescription(Party party, PartyCommand.ChangeDescription c) {
        party.updateDescription(c.descriptionMd());
        return party;
    }

    public Party changeBanner(Party party, PartyCommand.ChangeEmblem c) {
        party.updateBanner(c.emblemImageUrl(), c.emblemBgColor());
        return party;
    }

    public Party addTag(Party party, PartyCommand.TagOp c) {
        party.addTag(c.tag());
        return party;
    }

    public Party removeTag(Party party, PartyCommand.TagOp c) {
        party.removeTag(c.tag());
        return party;
    }

    // 가입/권한
    public void requestJoin(Party party, Long applicantId, PartyCommand.RequestJoin c) {
        party.requestJoin(applicantId, c.message());
    }

    public void approveJoin(Party party, PartyCommand.Approve c) {
        party.approveJoin(c.applicantPlayerId());
    }

    public void rejectJoin(Party party, PartyCommand.Reject c) {
        party.rejectJoin(c.applicantPlayerId());
    }

    public void cancelJoin(Party party, Long playerId) {
        party.cancelJoinRequest(playerId);
    }

    public void transferLeader(Party party, PartyCommand.TransferLeader c) {
        party.transferLeadership(c.fromLeaderPlayerId(), c.toPlayerId());
    }

    public void kick(Party party, PartyCommand.Kick c) {
        party.kickMember(c.targetPlayerId());
    }

    public void promote(Party party, PartyCommand.Promote c) {
        party.promoteOfficer(party.getLeaderPlayerId(), c.targetPlayerId());
    }

    public void demote(Party party, PartyCommand.Demote c) {
        party.demoteToMember(party.getLeaderPlayerId(), c.targetPlayerId());
    }

    public void leave(Party party, Long playerId) {
        party.leave(playerId);
    }

    public void disbandByLeader(Party party, Long leaderId) {
        party.disbandByLeader(leaderId);
    }

    // 초대
    public void invite(Party Party, Long inviterId, PartyCommand.Invite c) {
        Party.invite(inviterId, c.inviteePlayerId(), c.message(), parseDateTime(c.expiresAtIso()));
    }

    public void acceptInvitation(Party Party, Long playerId) {
        Party.acceptInvitation(playerId);
    }

    public void declineInvitation(Party Party, Long playerId) {
        Party.declineInvitation(playerId);
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
