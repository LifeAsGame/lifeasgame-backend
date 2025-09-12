package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerTitleError implements ErrorCode {

    PLAYER_TITLE_NOT_FOUND("PLT-404-NOT_FOUND", "Player title not found", 404),
    PLAYER_TITLE_ALREADY_EXISTS("PLT-404-ALREADY_EXISTS", "Player title already exists" , 409),
    ;

    private final String code;
    private final String message;
    private final int status;

    PlayerTitleError(String code, String message, int status) {
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
