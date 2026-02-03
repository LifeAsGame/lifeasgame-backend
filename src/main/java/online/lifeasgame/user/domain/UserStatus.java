package online.lifeasgame.user.domain;

import online.lifeasgame.core.lang.EnumParsers;
import online.lifeasgame.user.domain.error.UserError;

import java.util.List;

public enum UserStatus {
    ACTIVE, BANNED, DELETED;

    public static UserStatus parse(String raw) {
        return EnumParsers.parseStrict(
                UserStatus.class,
                raw,
                UserError.INVALID_USER_STATUS,
                "User status"
        );
    }

    public static UserStatus parseNullable(String raw) {
        if (raw == null) {
            return null;
        }

        return parse(raw);
    }

    public static List<UserStatus> parse(List<String> raw) {
        return EnumParsers.parseListStrict(
                UserStatus.class,
                raw,
                UserError.INVALID_USER_STATUS,
                "User statuses"
        );
    }
}
