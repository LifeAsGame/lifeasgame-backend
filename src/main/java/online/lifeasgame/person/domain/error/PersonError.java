package online.lifeasgame.person.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PersonError implements ErrorCode {
    INVALID_PERSON_DISPLAY_NAME("PER-400-INVALID-PERSON-DISPLAY-NAME", "Invalid Person display name", 400),
    INVALID_PERSON_CONTACT("PER-400-INVALID-PERSON-CONTACT", "Invalid Person contact", 400),
    PERSON_NOT_FOUND("PER-404-NOT-FOUND", "Person not found", 404),
    PERSON_ARCHIVED("PER-409-ARCHIVED", "Archived Person cannot be updated", 409);

    private final String code;
    private final String message;
    private final int status;

    PersonError(String code, String message, int status) {
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
