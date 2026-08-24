package online.lifeasgame.adminaudit.domain;

import java.util.regex.Pattern;

public record AdminAuditAction(String value) {

    private static final Pattern FORMAT = Pattern.compile(
            "[A-Z][A-Z0-9_]{2,63}"
    );

    public AdminAuditAction {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Admin audit action must be a 3-64 character uppercase code"
            );
        }
    }
}
