package online.lifeasgame.demo.domain;

import online.lifeasgame.core.error.ErrorCode;

public enum DemoError implements ErrorCode {
    DISABLED("DEMO-DISABLED", 503),
    INVALID_REQUEST("DEMO-INVALID-REQUEST", 400),
    PROOF_INVALID("DEMO-PROOF-INVALID", 401),
    SESSION_INVALID("DEMO-SESSION-INVALID", 401),
    ACTOR_FORBIDDEN("DEMO-ACTOR-FORBIDDEN", 403),
    RUN_NOT_FOUND("DEMO-RUN-NOT-FOUND", 404),
    NOT_READY("DEMO-NOT-READY", 409),
    KEY_CONFLICT("DEMO-KEY-CONFLICT", 409),
    RUN_EXPIRED("DEMO-RUN-EXPIRED", 410),
    CAPACITY_EXCEEDED("DEMO-CAPACITY-EXCEEDED", 429);

    private final String code;
    private final int status;

    DemoError(String code, int status) {
        this.code = code;
        this.status = status;
    }

    @Override public String code() { return code; }
    @Override public String message() { return code; }
    @Override public int status() { return status; }
}
