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
    PLAYER_EXP_AMOUNT_MUST_BE_POSITIVE(
            "PLR-400-EXP-AMOUNT-NOT-POSITIVE", "Experience grant amount must be positive", 400
    ),
    PLAYER_EXP_MUST_NOT_BE_NEGATIVE(
            "PLR-400-EXP-NEGATIVE", "Experience must not be negative", 400
    ),
    PLAYER_LEVELING_POLICY_REQUIRED(
            "PLR-400-LEVELING-POLICY-REQUIRED", "Leveling policy is required", 400
    ),
    PLAYER_EXP_OVERFLOW(
            "PLR-400-EXP-OVERFLOW", "Experience exceeds the supported range", 400
    ),
    PLAYER_GROWTH_REWARD_LINE_ID_REQUIRED(
            "PLR-400-GROWTH-REWARD-LINE-ID-REQUIRED", "Growth change requires a positive reward line id", 400
    ),
    PLAYER_GROWTH_CHANGE_INVALID(
            "PLR-400-GROWTH-CHANGE-INVALID", "Player growth change is invalid", 400
    ),
    PLAYER_GROWTH_CHANGE_INCONSISTENT(
            "PLR-409-GROWTH-CHANGE-INCONSISTENT", "Player growth change conflicts with its reward line", 409
    ),
    INVALID_TITLE("PLR-400-INVALID-TITLE", "Invalid title", 400),
    INVALID_STATUS_EFFECT_CODE("PLR-400-INVALID-STATUS-EFFECT", "Invalid status effect", 400);

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
