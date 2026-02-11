package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyStatus {
    ACTIVE, INACTIVE, DISBANDED;

    public static PartyStatus parse(String raw) {
        return EnumParsers.parseStrict(
                PartyStatus.class,
                raw,
                SocialError.INVALID_PARTY_STATUS,
                "Invalid party status"
        );
    }

    public static PartyStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
