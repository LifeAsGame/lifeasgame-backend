package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerError implements ErrorCode {
    INVALID_GENDER("PLR-400-INVALID-GENDER", "Invalid gender", 400),
    PLAYER_ALREADY_EXISTS("PLR-409-ALREADY_EXISTS", "Player already exists", 409),
    PLAYER_NOT_FOUND("PLR-404-NOT_FOUND", "Player not found", 404),
    INVALID_HP_CAPACITY("PLR-400-INVALID-HP-CAP", "Invalid hpDelta capacity", 400),
    INVALID_HP("PLR-400-INVALID-HP", "Invalid hpDelta", 400),
    INVALID_MP_CAPACITY("PLR-400-INVALID-MP-CAP", "Invalid mpDelta capacity", 400),
    INVALID_MP("PLR-400-INVALID-MP", "Invalid mpDelta", 400),
    INVALID_TITLE("PLR-400-INVALID-TITLE", "Invalid title", 400),;

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
