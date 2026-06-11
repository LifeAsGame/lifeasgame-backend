package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum ChannelRole {
    OWNER, MODERATOR, ADMIN, MEMBER;

    public static ChannelRole parse(String raw) {
        return EnumParsers.parseStrict(
                ChannelRole.class,
                raw,
                SocialError.INVALID_CHANNEL_ROLE,
                "channel role"
        );
    }

    public static ChannelRole parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
