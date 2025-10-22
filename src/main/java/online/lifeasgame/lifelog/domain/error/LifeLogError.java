package online.lifeasgame.lifelog.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum LifeLogError implements ErrorCode {
    MEDIA_NOT_FOUND("LIF-404-MEDIA-NOT-FOUND", "Media Not Found", 404),
    EXERCISE_NOT_FOUND("LIF-404-EXERCISE-NOT-FOUND", "Exercise Not Found", 404),
    COLLECTION_NOT_FOUND("LIF-404-COLLECTION-NOT-FOUND", "Collection Not Found", 404),
    DUPLICATED_ENTRY("LIF-409-DUPLICATE-ENTRY", "Duplicate Entry", 409),
    INVALID_STATE("LIF-400-INVALID-STATE", "Invalid State", 400),
    FORBIDDEN_ACCESS("LIF-403-FORBIDDEN-ACCESS", "Forbidden Access", 403),
    INVALID_MEDIA_CATEGORY("LIF-400-INVALID-MEDIA-CATEGORY", "Invalid Media Category", 400),
    INVALID_COLLECTION_CATEGORY("LIF-400-INVALID-COLLECTION-CATEGORY", "Invalid Collection Category", 400),
    INVALID_WATCH_STATUS("LIF-400-INVALID-WATCH-STATUS", "Invalid Watch Status", 400);

    private final String code;
    private final String message;
    private final int status;

    LifeLogError(String c, String m, int s) {
        this.code = c;
        this.message = m;
        this.status = s;
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
