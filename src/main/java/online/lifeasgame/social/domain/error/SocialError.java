package online.lifeasgame.social.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum SocialError implements ErrorCode {
    // Guild
    GUILD_NOT_FOUND("SOC-404-GUILD-NOT-FOUND","Guild Not Found",404),
    NOT_MEMBER("SOC-403-NOT-MEMBER","Not a member",403),
    LEADER_ONLY("SOC-403-LEADER-ONLY","Leader Only",403),
    OFFICER_OR_LEADER_ONLY("SOC-403-OFF-OR-LEADER","Officer or Leader Only",403),
    INVALID_STATE("SOC-400-INVALID-STATE","Invalid State",400),

    PARTY_NOT_FOUND("SOC-404-PARTY-NOT-FOUND","Party Not Found",404),

    FOLLOW_NOT_FOUND("SOC-404-FOLLOW-NOT-FOUND","Follow Not Found",404);

    private final String code;
    private final String message;
    private final int status;

    SocialError(String c, String m, int s) {
        this.code = c;
        this.message = m;
        this.status = s;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public int status() {
        return status;
    }
}
