package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum LevelingError implements ErrorCode {

    LEVEL_CURVE_OVERFLOW("LVL-500-OVERFLOW", "Level curve overflow during bootstrap", 500);

    private final String code;
    private final String message;
    private final int status;

    LevelingError(String code, String message, int status) {
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
