package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyWaitStatus {
    PENDING, APPROVED, REJECTED, CANCELLED;

    public static PartyWaitStatus parse(String raw) {
        return EnumParsers.parseStrict(
                PartyWaitStatus.class,
                raw,
                SocialError.INVALID_PARTY_WAIT_STATUS,
                "Invalid party wait status"
        );
    }

    public static PartyWaitStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
