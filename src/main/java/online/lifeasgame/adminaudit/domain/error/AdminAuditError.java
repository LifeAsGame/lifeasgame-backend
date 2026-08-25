package online.lifeasgame.adminaudit.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum AdminAuditError implements ErrorCode {
    DUPLICATE_IDEMPOTENCY_KEY(
            "ADMIN-AUDIT-IDEMPOTENCY-CONFLICT",
            "Admin command idempotency key already committed",
            409
    );

    private final String code;
    private final String message;
    private final int status;

    AdminAuditError(String code, String message, int status) {
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
