package online.lifeasgame.core.error;

public interface ErrorCode {
    String code();
    String message();
    int status();
    default Sensitivity sensitivity() { return Sensitivity.NONE; }
}
