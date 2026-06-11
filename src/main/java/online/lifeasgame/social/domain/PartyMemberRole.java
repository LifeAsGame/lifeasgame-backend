package online.lifeasgame.social.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.social.domain.error.SocialError;

public enum PartyMemberRole {
    LEADER, OFFICER, MEMBER;

    public static PartyMemberRole parse(String raw) {
        return EnumParsers.parseStrict(
                PartyMemberRole.class,
                raw,
                SocialError.INVALID_PARTY_MEMBER_ROLE,
                "Invalid party member role"
        );
    }

    public static PartyMemberRole parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }
}
