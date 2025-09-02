package online.lifeasgame.core.error;

import java.util.Objects;

public class BaseException extends RuntimeException {

    private final ErrorCode ec;
    private final String detail;

    public BaseException(ErrorCode ec) {
        this(ec, null, null);
    }

    public BaseException(ErrorCode ec, String detail) {
        this(ec, detail, null);
    }

    public BaseException(ErrorCode ec, String detail, Throwable cause) {
        super(detail != null ? detail : Objects.requireNonNull(ec, "errorCode").message(), cause);
        this.ec = Objects.requireNonNull(ec, "errorCode");
        this.detail = detail;
    }

    public ErrorCode getErrorCode() {
        return ec;
    }

    public String detail() {
        return detail;
    }

    public String code() { return ec.code(); }
    public int status()  { return ec.status(); }
    public String message() { return ec.message(); }

    @Override
    public String toString() {
        return "BaseException{" +
                "code='" + ec.code() + '\'' +
                ", status=" + ec.status() +
                ", message='" + ec.message() + '\'' +
                (detail != null ? ", detail='" + detail + '\'' : "") +
                '}';
    }
}
