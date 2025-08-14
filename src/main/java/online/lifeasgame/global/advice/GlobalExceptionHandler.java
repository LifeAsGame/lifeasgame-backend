package online.lifeasgame.global.advice;

import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.shared.error.BaseException;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBase(BaseException ex, WebRequest req) {
        var ec = ex.getErrorCode();
        var pd = ProblemDetail.forStatus(ec.status());

        pd.setTitle(ec.code());
        pd.setDetail(ec.message());
        pd.setProperty("path", ((ServletWebRequest)req).getRequest().getRequestURI());
        pd.setProperty("traceId", MDC.get("traceId"));

        return ResponseEntity.status(ec.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleEtc(Exception ex, WebRequest req) {
        var pd = ProblemDetail.forStatus(500);

        pd.setTitle("GEN-000");
        pd.setDetail("Internal server error");
        pd.setProperty("path", ((ServletWebRequest)req).getRequest().getRequestURI());
        pd.setProperty("traceId", MDC.get("traceId"));

        log.error("Unhandled error", ex);
        return ResponseEntity.status(500).body(pd);
    }
}
