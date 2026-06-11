package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildVisibility {
    PUBLIC, PRIVATE;

    public static GuildVisibility parse(String raw) {
        return EnumParsers.parseStrict(
                GuildVisibility.class,
                raw,
                SocialError.INVALID_GUILD_VISIBILITY,
                "Invalid guild visibility"
        );
    }

    public static GuildVisibility parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
