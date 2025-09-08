package online.lifeasgame.core.error;

public class ConfigException extends BaseException {

    public ConfigException(ErrorCode error) {
        super(error);
    }

    public ConfigException(ErrorCode error, String detail) {
        super(error, detail);
    }

    public ConfigException(ErrorCode error, String detail, Throwable cause) {
        super(error, detail, cause);
    }
}
