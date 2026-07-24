package online.lifeasgame.lifelog.quick.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum QuickRecordError implements ErrorCode {
    INVALID_REQUEST(
            "LIF-400-INVALID-QUICK-RECORD",
            "Invalid quick record request",
            400
    ),
    IDEMPOTENCY_KEY_REQUIRED(
            "IDEM-400-KEY-REQUIRED",
            "Idempotency-Key is required",
            400
    ),
    IDEMPOTENCY_KEY_PAYLOAD_CONFLICT(
            "IDEM-409-KEY-PAYLOAD-CONFLICT",
            "Idempotency-Key was already used for a different request",
            409
    );

    private final String code;
    private final String message;
    private final int status;

    QuickRecordError(String code, String message, int status) {
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
