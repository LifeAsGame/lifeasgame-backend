package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyJoinPolicy {
    OPEN, APPROVAL, INVITE_ONLY;

    public static PartyJoinPolicy parse(String raw) {
        return EnumParsers.parseStrict(
                PartyJoinPolicy.class,
                raw,
                SocialError.INVALID_PARTY_JOIN_POLICY,
                "Invalid party join policy"
        );
    }

    public static PartyJoinPolicy parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
