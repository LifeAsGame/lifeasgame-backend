package online.lifeasgame.social.application.model;


import online.lifeasgame.social.application.command.PartyCommand;
import online.lifeasgame.social.domain.PartyJoinPolicy;
import online.lifeasgame.social.domain.PartyVisibility;

public final class PartySpec {
    public record Create(
            Long playerId,
            String name,
            String code,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
            PartyVisibility visibility,
            PartyJoinPolicy joinPolicy,
            int maxMembers
    ) {
        public static Create from(Long playerId, PartyCommand.Create c) {
            return new Create(
                    playerId,
                    c.name(),
                    c.code(),
                    c.descriptionMd(),
                    c.bannerImageUrl(),
                    c.bannerBgColor(),
                    c.visibility() == null ? null : PartyVisibility.valueOf(c.visibility()),
                    c.joinPolicy() == null ? null : PartyJoinPolicy.valueOf(c.joinPolicy()),
                    c.maxMembers()
            );
        }
    }
}
