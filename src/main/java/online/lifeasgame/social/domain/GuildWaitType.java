package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildWaitType {
    JOIN_REQUEST, INVITATION;

    public static GuildWaitType parse(String raw) {
        return EnumParsers.parseStrict(
                GuildWaitType.class,
                raw,
                SocialError.INVALID_GUILD_WAIT_TYPE,
                "Invalid guild wait type"
        );
    }

    public static GuildWaitType parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
