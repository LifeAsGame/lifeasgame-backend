package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum FollowState {
    FOLLOWING, STOPPED;

    public static FollowState parse(String raw) {
        return EnumParsers.parseStrict(
                FollowState.class,
                raw,
                SocialError.INVALID_FOLLOW_STATE,
                "Invalid follow state"
        );
    }

    public static FollowState parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }


}
