package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerError implements ErrorCode {
    PLAYER_ALREADY_EXISTS("","",0);

    private final String code;
    private final String message;
    private final int status;

    PlayerError(String code, String message, int status) {
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
