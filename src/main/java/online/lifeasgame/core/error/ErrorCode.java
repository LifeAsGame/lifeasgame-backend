package online.lifeasgame.core.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();
    String message();
    HttpStatus status();
}
