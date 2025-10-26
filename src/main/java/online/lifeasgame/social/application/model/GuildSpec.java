package online.lifeasgame.social.application.model;


import online.lifeasgame.social.application.command.GuildCommand;
import online.lifeasgame.social.domain.GuildJoinPolicy;
import online.lifeasgame.social.domain.GuildVisibility;

public final class GuildSpec {
    public record Create(
            Long playerId,
            String name,
            String code,
            String descriptionMd,
            String emblemImageUrl,
            String emblemBgColor,
            GuildVisibility visibility,
            GuildJoinPolicy joinPolicy,
            int maxMembers
    ) {
        public static Create from(Long playerId, GuildCommand.Create c) {
            return new Create(
                    playerId,
                    c.name(),
                    c.code(),
                    c.descriptionMd(),
                    c.emblemImageUrl(),
                    c.emblemBgColor(),
                    c.visibility() == null ? null : GuildVisibility.valueOf(c.visibility()),
                    c.joinPolicy() == null ? null : GuildJoinPolicy.valueOf(c.joinPolicy()),
                    c.maxMembers()
            );
        }
    }
}
