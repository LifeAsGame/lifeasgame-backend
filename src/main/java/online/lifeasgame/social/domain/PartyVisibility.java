package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyVisibility {
    PUBLIC, PRIVATE;

    public static PartyVisibility parse(String raw) {
        return EnumParsers.parseStrict(
                PartyVisibility.class,
                raw,
                SocialError.INVALID_PARTY_VISIBILITY,
                "Invalid party visibility"
        );
    }

    public static PartyVisibility parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
