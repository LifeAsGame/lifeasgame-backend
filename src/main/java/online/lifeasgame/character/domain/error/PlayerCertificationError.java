package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PlayerCertificationError implements ErrorCode {

    EXPIRES_BEFORE_ACQUIRED("PCF-422-EXPIRES-BEFORE-ACQUIRED", "Expires date must be on/after acquired date", 422),
    PLAYER_CERTIFICATION_NOT_FOUND("PCD-404-PLAYER_CERTIFICATION-NOT_FOUND", "Player certification not found", 404)
    ;

    private final String code;
    private final String message;
    private final int status;

    PlayerCertificationError(String code, String message, int status) {
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
