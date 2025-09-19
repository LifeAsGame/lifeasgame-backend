package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum HobbyError implements ErrorCode {
    INVALID_HOBBY_CATEGORY("HOB-400-INVALID-HOBBY-CATEGORY", "Invalid hobby category", 400),
    HOBBY_NOT_FOUND("HOB-404-NOT_FOUND", "Hobby not found", 404)
    ;

    private final String code;
    private final String message;
    private final int status;

    HobbyError(String code, String message, int status) {
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
