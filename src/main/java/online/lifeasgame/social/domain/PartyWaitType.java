package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyWaitType {
    JOIN_REQUEST, INVITATION;

    public static PartyWaitType parse(String raw) {
        return EnumParsers.parseStrict(
                PartyWaitType.class,
                raw,
                SocialError.INVALID_PARTY_WAIT_TYPE,
                "Invalid party wait type"
        );
    }

    public static PartyWaitType parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
