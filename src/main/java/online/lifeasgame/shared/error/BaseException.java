package online.lifeasgame.shared.error;

public class BaseException extends RuntimeException {

    private final ErrorCode ec;

    public BaseException(ErrorCode ec, Throwable cause) {
        super(ec.message(), cause);
        this.ec = ec;
    }
    public BaseException(ErrorCode ec) {
        this(ec, null);
    }

    public ErrorCode getErrorCode() {
        return ec;
    }
}
