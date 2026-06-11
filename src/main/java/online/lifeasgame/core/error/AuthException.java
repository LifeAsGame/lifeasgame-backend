package online.lifeasgame.core.error;

public class AuthException extends BaseException {

    public AuthException(ErrorCode error) {
        super(error);
    }

    public AuthException(ErrorCode error, String detail) {
        super(error, detail);
    }

    public AuthException(ErrorCode error, String detail, Throwable cause) {
        super(error, detail, cause);
    }
}
