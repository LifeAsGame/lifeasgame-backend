package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerHobbyError implements ErrorCode {

    PLAYER_HOBBY_NOT_FOUND("PLH-404-PLAYER_HOBBY-NOT_FOUND", "Player Hobby not found", 404),
    INVALID_PLAYER_HOBBY_STATUS("PLH-400-INVALID-PLAYER_HOBBY-STATUS", "Invalid player hobby", 400),
    ;

    private final String code;
    private final String message;
    private final int status;

    PlayerHobbyError(String code, String message, int status) {
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
