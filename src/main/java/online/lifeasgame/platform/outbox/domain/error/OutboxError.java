package online.lifeasgame.platform.outbox.domain.error;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.ErrorCode;

@RequiredArgsConstructor
public enum OutboxError implements ErrorCode {

    OUTBOX_EVENT_NOT_FOUND(
            "OUT-404-EVENT-NOT-FOUND",
            "Outbox event was not found",
            404
    ),
    OUTBOX_EVENT_TYPE_UNKNOWN(
            "OUT-400-EVENT-TYPE-UNKNOWN",
            "Outbox event type is not registered",
            400
    ),
    OUTBOX_EVENT_CODEC_FAILED(
            "OUT-500-EVENT-CODEC-FAILED",
            "Outbox event payload could not be encoded or decoded",
            500
    ),
    OUTBOX_EVENT_STATE_INVALID(
            "OUT-409-EVENT-STATE-INVALID",
            "Outbox event is not in the required state",
            409
    ),
    OUTBOX_EVENT_LOCK_OWNER_MISMATCH(
            "OUT-409-EVENT-LOCK-OWNER-MISMATCH",
            "Outbox event is owned by another relay instance",
            409
    ),
    OUTBOX_EVENT_ATTRIBUTE_TYPE_UNSUPPORTED(
            "OUT-400-EVENT-ATTRIBUTE-TYPE-UNSUPPORTED",
            "Outbox event attribute type is not supported",
            400
    );

    private final String code;
    private final String message;
    private final int status;

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
