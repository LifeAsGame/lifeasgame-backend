package online.lifeasgame.role.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum RoleError implements ErrorCode {
    INVALID_ROLE_TYPE("ROL-400-INVALID-ROLE-TYPE", "Invalid Role type", 400),
    INVALID_ROLE_NAME("ROL-400-INVALID-ROLE-NAME", "Invalid Role name", 400),
    INVALID_ROLE_DESCRIPTION("ROL-400-INVALID-ROLE-DESCRIPTION", "Invalid Role description", 400),
    INVALID_ROLE_RELATION_TYPE("ROL-400-INVALID-ROLE-RELATION-TYPE", "Invalid Role relation type", 400),
    ROLE_NOT_FOUND("ROL-404-NOT-FOUND", "Role not found", 404),
    ROLE_RELATION_NOT_FOUND("ROL-404-RELATION-NOT-FOUND", "Role relation not found", 404),
    ROLE_ARCHIVED("ROL-409-ARCHIVED", "Archived Role cannot be updated", 409),
    ROLE_RELATION_ALREADY_EXISTS("ROL-409-RELATION-ALREADY-EXISTS", "Role relation already exists", 409),
    ROLE_RELATION_ARCHIVED("ROL-409-RELATION-ARCHIVED", "Archived Role relation cannot be updated", 409),
    ROLE_EVENT_NOT_FOUND("ROL-404-EVENT-NOT-FOUND", "Role event not found", 404),
    ROLE_EVENT_PARTICIPANT_NOT_FOUND("ROL-404-EVENT-PARTICIPANT-NOT-FOUND", "Role event participant not found", 404),
    INVALID_ROLE_EVENT_TITLE("ROL-400-INVALID-EVENT-TITLE", "Invalid Role event title", 400),
    INVALID_ROLE_EVENT_DESCRIPTION("ROL-400-INVALID-EVENT-DESCRIPTION", "Invalid Role event description", 400),
    INVALID_ROLE_EVENT_TIME_RANGE("ROL-400-INVALID-EVENT-TIME-RANGE", "Invalid Role event time range", 400),
    INVALID_ROLE_EVENT_PARTICIPANT_TYPE("ROL-400-INVALID-EVENT-PARTICIPANT-TYPE", "Invalid Role event participant type", 400),
    ROLE_EVENT_NOT_PLANNED("ROL-409-EVENT-NOT-PLANNED", "Only planned Role events can be changed", 409),
    ROLE_EVENT_PARTICIPANT_ALREADY_EXISTS("ROL-409-EVENT-PARTICIPANT-ALREADY-EXISTS", "Role event participant already exists", 409);

    private final String code;
    private final String message;
    private final int status;

    RoleError(String code, String message, int status) {
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
