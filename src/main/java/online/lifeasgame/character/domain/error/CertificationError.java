package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum CertificationError implements ErrorCode {
    INVALID_CERTIFICATION_CATEGORY("CER-400-INVALID-CERTIFICATION-CATEGORY", "Invalid Certification category", 400),
    CERTIFICATION_NOT_FOUND("CER-404-NOT_FOUND", "Certification not found", 404);

    private final String code;
    private final String message;
    private final int status;

    CertificationError(String code, String message, int status) {
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
