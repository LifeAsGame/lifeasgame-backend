package online.lifeasgame.notification.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum NotificationError implements ErrorCode {
    NOTIFICATION_NOT_FOUND(
            "NTF-404-NOT-FOUND", "Notification not found", 404
    ),
    PLAYER_ID_REQUIRED(
            "NTF-400-PLAYER-ID-REQUIRED", "Notification player id is required", 400
    ),
    SOURCE_EVENT_ID_REQUIRED(
            "NTF-400-SOURCE-EVENT-ID-REQUIRED", "Notification source event id is required", 400
    ),
    SOURCE_EVENT_ID_TOO_LONG(
            "NTF-400-SOURCE-EVENT-ID-TOO-LONG", "Notification source event id is too long", 400
    ),
    TYPE_REQUIRED(
            "NTF-400-TYPE-REQUIRED", "Notification type is required", 400
    ),
    TITLE_REQUIRED(
            "NTF-400-TITLE-REQUIRED", "Notification title is required", 400
    ),
    TITLE_TOO_LONG(
            "NTF-400-TITLE-TOO-LONG", "Notification title is too long", 400
    ),
    BODY_REQUIRED(
            "NTF-400-BODY-REQUIRED", "Notification body is required", 400
    ),
    OCCURRED_AT_REQUIRED(
            "NTF-400-OCCURRED-AT-REQUIRED", "Notification occurred time is required", 400
    ),
    READ_AT_REQUIRED(
            "NTF-400-READ-AT-REQUIRED", "Notification read time is required", 400
    ),
    PAGE_SIZE_INVALID(
            "NTF-400-PAGE-SIZE-INVALID", "Notification page size must be between 1 and 100", 400
    );

    private final String code;
    private final String message;
    private final int status;

    NotificationError(String code, String message, int status) {
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
