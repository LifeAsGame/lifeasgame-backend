package online.lifeasgame.social.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum SocialError implements ErrorCode {
    GUILD_NOT_FOUND("SOC-404-GUILD-NOT-FOUND","Guild Not Found",404),
    NOT_MEMBER("SOC-403-NOT-MEMBER","Not a member",403),
    LEADER_ONLY("SOC-403-LEADER-ONLY","Leader Only",403),
    OFFICER_OR_LEADER_ONLY("SOC-403-OFF-OR-LEADER","Officer or Leader Only",403),
    INVALID_STATE("SOC-400-INVALID-STATE","Invalid State",400),

    PARTY_NOT_FOUND("SOC-404-PARTY-NOT-FOUND","Party Not Found",404),

    FOLLOW_NOT_FOUND("SOC-404-FOLLOW-NOT-FOUND","Follow Not Found",404),
    CONNECTION_PEER_NOT_FOUND("SOC-404-CONNECTION-PEER-NOT-FOUND", "Connection peer not found", 404),
    NOT_FRIEND("SOC-404-NOT-FRIEND", "Friend Not Found", 404),

    CHAT_CHANNEL_NOT_FOUND("SOC-404-CHANNEL-NOT-FOUND","Chat Channel Not Found",404),
    CHAT_CHANNEL_FORBIDDEN("SOC-403-CHANNEL-FORBIDDEN","Channel access denied",403),
    CHAT_CHANNEL_READ_ONLY("SOC-403-CHANNEL-READ-ONLY","Channel is read only",403),
    INVALID_CHANNEL_ROLE("SOC-400-INVALID-CHANNEL-ROLE", "Invalid Channel Role", 400),
    INVALID_CHANNEL_TYPE("SOC-400-INVALID-CHANNEL-TYPE", "Invalid Channel Type", 400),
    INVALID_GUILD_JOIN_POLICY("SOC-400-INVALID-GUILD-JOIN-POLICY", "Invalid Guild Join Policy", 400),
    INVALID_GUILD_MEMBER_ROLE("SOC-400-INVALID-GUILD-MEMBER-ROLE", "Invalid guild member role", 400),
    INVALID_GUILD_STATUS("SOC-400-INVALID-GUILD-STATUS", "Invalid guild status", 400),
    INVALID_GUILD_VISIBILITY("SOC-400-INVALID-GUILD-VISIBILITY", "Invalid guild visibility", 400),
    INVALID_GUILD_WAIT_STATUS("SOC-400-INVALID-GUILD-STATUS", "Invalid guild wait status", 400),
    INVALID_GUILD_WAIT_TYPE("SOC-400-INVALID-GUILD-WAIT-TYPE", "Invalid guild wait type", 400),
    INVALID_PARTY_JOIN_POLICY("SOC-400-INVALID-PARTY-JOIN-POLICY", "Invalid party join policy", 400),
    INVALID_PARTY_MEMBER_ROLE("SOC-400-INVALID-PARTY-MEMBER-ROLE", "Invalid party member role", 400),
    INVALID_PARTY_STATUS("SOC-400-INVALID-PARTY-STATUS", "Invalid party status", 400),
    INVALID_PARTY_VISIBILITY("SOC-400-INVALID-PARTY-VISIBILITY", "Invalid party visibility", 400),
    INVALID_PARTY_WAIT_STATUS("SOC-400-INVALID-PARTY-WAIT-STATUS", "Invalid party wait status", 400),
    INVALID_PARTY_WAIT_TYPE("SOC-400-INVALID-PARTY-WAIT-TYPE", "Invalid party wait type", 400),
    INVALID_FOLLOW_STATE("SOC-400-INVALID-FOLLOW-STATE", "Invalid follow state", 400);

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
