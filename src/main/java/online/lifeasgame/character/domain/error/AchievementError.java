package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum AchievementError implements ErrorCode {
    INVALID_ACHIEVEMENT_CATEGORY("ACH-400-INVALID-ACHIEVEMENT-CATEGORY", "Invalid Achievement category", 400),
    ACHIEVEMENT_NOT_FOUND("ACH-404-NOT_FOUND", "Achievement not found", 404);

    private final String code;
    private final String message;
    private final int status;

    AchievementError(String code, String message, int status) {
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
