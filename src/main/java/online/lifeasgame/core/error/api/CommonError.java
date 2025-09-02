package online.lifeasgame.core.error.api;

import online.lifeasgame.core.error.ErrorCode;

public enum CommonError implements ErrorCode {
    GEN_000("GEN-000", "Internal server error", 500),
    NOT_FOUND("NOT-FOUND", "Resource not found", 404),
    REQ_VALIDATION("REQ-VALIDATION", "Validation failed", 400),
    REQ_BAD_INPUT("REQ-BAD-INPUT", "Malformed request", 400),
    DATA_DUPLICATE("DATA-DUPLICATE", "Duplicate key", 409),
    DATA_INTEGRITY("DATA-INTEGRITY", "Data integrity violation", 400);

    private final String code;
    private final String message;
    private final int status;

    CommonError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code()   { return code; }
    public String message(){ return message; }
    public int status(){ return status; }
}
