package online.lifeasgame.core.error;

public class DomainException extends BaseException {

    public DomainException(ErrorCode error) {
        super(error);
    }

    public DomainException(ErrorCode error, String detail) {
        super(error, detail);
    }

    public DomainException(ErrorCode error, String detail, Throwable cause) {
        super(error, detail, cause);
    }
}
