package online.lifeasgame.user.domain.error;

import online.lifeasgame.core.error.ErrorCode;

public enum UserSettingError implements ErrorCode {
    USER_SETTING_NOT_FOUND("USS_404_NOT_FOUND", "UserSetting Not Found", 404);


    private final String code;
    private final String message;
    private final int status;

    UserSettingError(String code, String message, int status) {
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
