package online.lifeasgame.user.domain.error;

import online.lifeasgame.core.error.ErrorCode;
import online.lifeasgame.core.error.Sensitivity;

public enum UserError implements ErrorCode {

    EMAIL_DUPLICATE("USR-409-EMAIL-DUP", "Email already in use", 409) {
        @Override
        public Sensitivity sensitivity() {
            return Sensitivity.PII;
        }
    },
    USER_NOT_FOUND("USR-404-NOT-FOUND", "User not found", 404),
    NICKNAME_DUPLICATE("USR-409-NICKNAME-DUP", "Nickname already in use", 409),
    INCORRECT_PASSWORD("USR-400-INVALID-CURRENT-PASSWORD", "Current password is incorrect", 400),
    INVALID_USER_STATUS("USR-400-INVALID-STATUS", "Invalid user status", 400),
    ;

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
