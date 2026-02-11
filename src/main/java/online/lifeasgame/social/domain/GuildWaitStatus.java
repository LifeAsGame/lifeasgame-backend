package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum GuildWaitStatus {
    PENDING, APPROVED, REJECTED, CANCELLED;

    public static GuildWaitStatus parse(String raw) {
        return EnumParsers.parseStrict(
                GuildWaitStatus.class,
                raw,
                SocialError.INVALID_GUILD_WAIT_STATUS,
                "Invalid guild wait status"
        );
    }

    public static GuildWaitStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
