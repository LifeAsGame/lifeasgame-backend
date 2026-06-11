package online.lifeasgame.core.error.api;

import online.lifeasgame.core.error.ErrorCode;

public enum AuthError implements ErrorCode {
    UNAUTHORIZED("AUTH-UNAUTHORIZED", "Authentication required", 401),
    FORBIDDEN("AUTH-FORBIDDEN", "Forbidden", 403),
    BAD_CREDENTIALS("AUTH-BAD-CREDENTIALS", "Invalid email or password", 401),
    TOKEN_EXPIRED("AUTH-TOKEN-EXPIRED", "Token expired", 401),
    TOKEN_INVALID("AUTH-TOKEN-INVALID", "Invalid token", 401);

    private final String code;
    private final String message;
    private final int status;

    AuthError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int status() {
        return status;
    }
}
