package online.lifeasgame.social.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.application.model.GuildSpec;
import online.lifeasgame.social.domain.Guild;
import online.lifeasgame.social.domain.GuildJoinPolicy;
import online.lifeasgame.social.domain.GuildVisibility;
import online.lifeasgame.social.domain.error.SocialError;
import online.lifeasgame.social.domain.repository.GuildRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class GuildWriter {

    private final GuildRepository guildRepository;

    public Guild create(GuildSpec.Create spec) {
        return guildRepository.save(Guild.create(
                spec.playerId(),
                spec.name(),
                spec.code(),
                spec.descriptionMd(),
                spec.emblemImageUrl(),
                spec.emblemBgColor(),
                spec.visibility(),
                spec.joinPolicy(),
                spec.maxMembers()
        ));
    }

    public Guild rename(Guild guild, GuildCommand.Rename c) {
        guild.rename(c.name());
        return guild;
    }

    public Guild changePolicy(Guild guild, GuildCommand.ChangePolicy c) {
        if (c.visibility() != null) {
            guild.changeVisibility(GuildVisibility.valueOf(c.visibility()));
        }
        if (c.joinPolicy() != null) {
            guild.changeJoinPolicy(GuildJoinPolicy.valueOf(c.joinPolicy()));
        }
        if (c.maxMembers() > 0) {
            guild.changeMaxMembers(c.maxMembers());
        }
        return guild;
    }

    public Guild changeDescription(Guild guild, GuildCommand.ChangeDescription c) {
        guild.updateDescription(c.descriptionMd());
        return guild;
    }

    public Guild changeEmblem(Guild guild, GuildCommand.ChangeEmblem c) {
        guild.updateEmblem(c.emblemImageUrl(), c.emblemBgColor());
        return guild;
    }

    public Guild addTag(Guild guild, GuildCommand.TagOp c) {
        guild.addTag(c.tag());
        return guild;
    }

    public Guild removeTag(Guild guild, GuildCommand.TagOp c) {
        guild.removeTag(c.tag());
        return guild;
    }

    // 가입/권한
    public void requestJoin(Guild guild, Long applicantId, GuildCommand.RequestJoin c) {
        guild.requestJoin(applicantId, c.message());
    }

    public void approveJoin(Guild guild, GuildCommand.Approve c) {
        guild.approveJoin(c.applicantPlayerId());
    }

    public void rejectJoin(Guild guild, GuildCommand.Reject c) {
        guild.rejectJoin(c.applicantPlayerId());
    }

    public void cancelJoin(Guild guild, Long playerId) {
        guild.cancelJoinRequest(playerId);
    }

    public void transferLeader(Guild guild, GuildCommand.TransferLeader c) {
        guild.transferLeadership(c.fromLeaderPlayerId(), c.toPlayerId());
    }

    public void kick(Guild guild, GuildCommand.Kick c) {
        guild.kickMember(c.targetPlayerId());
    }

    public void promote(Guild guild, GuildCommand.Promote c) {
        guild.promoteOfficer(guild.getLeaderPlayerId(), c.targetPlayerId());
    }

    public void demote(Guild guild, GuildCommand.Demote c) {
        guild.demoteToMember(guild.getLeaderPlayerId(), c.targetPlayerId());
    }

    public void leave(Guild guild, Long playerId) {
        guild.leave(playerId);
    }

    public void disbandByLeader(Guild guild, Long leaderId) {
        guild.disbandByLeader(leaderId);
    }

    // 초대
    public void invite(Guild guild, Long inviterId, GuildCommand.Invite c) {
        guild.invite(inviterId, c.inviteePlayerId(), c.message(), parseDateTime(c.expiresAtIso()));
    }

    public void acceptInvitation(Guild guild, Long playerId) {
        guild.acceptInvitation(playerId);
    }

    public void declineInvitation(Guild guild, Long playerId) {
        guild.declineInvitation(playerId);
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
