package online.lifeasgame.user.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum UserError implements ErrorCode {

    EMAIL_DUPLICATE("USR-409-EMAIL_DUP", "Email already in use", 409),
    USER_NOT_FOUND("USR-404-NOT_FOUND", "User not found", 404);

    private final String code;
    private final String message;
    private final int status;

    UserError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
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
