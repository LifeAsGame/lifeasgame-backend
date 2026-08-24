package online.lifeasgame.adminaudit.domain;

import java.util.regex.Pattern;

public record AdminAuditTargetType(String value) {

    private static final Pattern FORMAT = Pattern.compile(
            "[A-Z][A-Z0-9_]{2,63}"
    );

    public AdminAuditTargetType {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Admin audit target type must be a 3-64 character uppercase code"
            );
        }
    }
}
