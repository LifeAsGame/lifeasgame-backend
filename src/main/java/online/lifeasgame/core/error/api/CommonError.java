package online.lifeasgame.core.error.api;

import online.lifeasgame.core.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CommonError implements ErrorCode {
    GEN_000("GEN-000", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND("NOT-FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    REQ_VALIDATION("REQ-VALIDATION", "Validation failed", HttpStatus.BAD_REQUEST),
    REQ_BAD_INPUT("REQ-BAD-INPUT", "Malformed request", HttpStatus.BAD_REQUEST),
    DATA_DUPLICATE("DATA-DUPLICATE", "Duplicate key", HttpStatus.CONFLICT),
    DATA_INTEGRITY("DATA-INTEGRITY", "Data integrity violation", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    CommonError(String code, String message, HttpStatus status) {
        this.code = code; this.message = message; this.status = status;
    }
    public String code()   { return code; }
    public String message(){ return message; }
    public HttpStatus status(){ return status; }
}
