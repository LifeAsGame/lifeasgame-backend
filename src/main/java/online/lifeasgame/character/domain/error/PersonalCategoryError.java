package online.lifeasgame.character.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum PersonalCategoryError implements ErrorCode {
    INVALID_NAME("PCA-400-INVALID-NAME", "Invalid personal category name", 400),
    INVALID_ASSIGNMENT("PCA-400-INVALID-ASSIGNMENT", "personalCategoryId is required", 400),
    NOT_FOUND("PCA-404-NOT-FOUND", "Personal category not found", 404),
    DUPLICATE_NAME("PCA-409-DUPLICATE-NAME", "Personal category name already exists", 409),
    OWNED_ITEM_NOT_FOUND("PCA-404-OWNED-ITEM-NOT-FOUND", "Owned item not found", 404);

    private final String code;
    private final String message;
    private final int status;

    PersonalCategoryError(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    public String code() { return code; }
    public String message() { return message; }
    public int status() { return status; }
}
