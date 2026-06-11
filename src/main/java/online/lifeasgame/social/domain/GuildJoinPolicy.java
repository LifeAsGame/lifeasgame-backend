package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildJoinPolicy {
    OPEN, APPROVAL, INVITE_ONLY;

    public static GuildJoinPolicy parse(String raw) {
        return EnumParsers.parseStrict(
                GuildJoinPolicy.class,
                raw,
                SocialError.INVALID_GUILD_JOIN_POLICY,
                "Invalid guild join policy"
        );
    }

    public static GuildJoinPolicy parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
