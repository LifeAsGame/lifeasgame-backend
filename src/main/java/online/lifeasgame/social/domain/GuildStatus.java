package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildStatus {
    ACTIVE, INACTIVE, DISBANDED;

    public static GuildStatus parse(String raw) {
        return EnumParsers.parseStrict(
                GuildStatus.class,
                raw,
                SocialError.INVALID_GUILD_STATUS,
                "Invalid guild status"
        );
    }

    public static GuildStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
