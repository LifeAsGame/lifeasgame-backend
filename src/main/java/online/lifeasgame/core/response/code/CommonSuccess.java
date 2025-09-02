package online.lifeasgame.core.response.code;

public enum CommonSuccess implements SuccessCode {
    OK(200, "COMMON-200", "성공입니다."),
    CREATED(201, "COMMON-201", "생성에 성공했습니다."),
    DELETED(200, "COMMON-202", "삭제에 성공했습니다.");

    private final int status;
    private final String code;
    private final String message;

    CommonSuccess(int status, String code, String message) {
        this.status = status; this.code = code; this.message = message;
    }
    @Override public String code()    { return code; }
    @Override public String message() { return message; }
    @Override public int status()     { return status; }
}
