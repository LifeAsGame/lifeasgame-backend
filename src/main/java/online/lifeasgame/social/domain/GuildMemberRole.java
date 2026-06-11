package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildMemberRole {
    LEADER, OFFICER, MEMBER;

    public static GuildMemberRole parse(String raw) {
        return EnumParsers.parseStrict(
                GuildMemberRole.class,
                raw,
                SocialError.INVALID_GUILD_MEMBER_ROLE,
                "Invalid guild member role"
        );
    }

    public static GuildMemberRole parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
