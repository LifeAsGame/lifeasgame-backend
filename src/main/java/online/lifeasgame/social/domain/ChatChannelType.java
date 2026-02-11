package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum ChatChannelType {
    GLOBAL,
    GUILD,
    PARTY,
    ADMIN,
    FRIEND,
    SYSTEM;

    public static ChatChannelType parse(String raw) {
        return EnumParsers.parseStrict(
                ChatChannelType.class,
                raw,
                SocialError.INVALID_CHANNEL_TYPE,
                "channel role"
        );
    }

    public static ChatChannelType parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
