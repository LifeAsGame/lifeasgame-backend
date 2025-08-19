package online.lifeasgame.global.error.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.global.error.api.CommonError;
import online.lifeasgame.global.error.api.ErrorKeys;
import online.lifeasgame.global.error.handler.config.AppErrorProperties;
import online.lifeasgame.global.error.handler.factory.ProblemDetailFactory;
import online.lifeasgame.global.error.handler.mapper.FieldErrorMapper;
import online.lifeasgame.global.error.handler.mapper.ViolationMapper;
import online.lifeasgame.shared.error.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ProblemDetailFactory pdf;
    private final AppErrorProperties props;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBase(BaseException ex, WebRequest req) {
        var ec = ex.getErrorCode();
        var pd = pdf.base(ec.status(), ec.message(), ex.getMessage(), ec.code(), req);

        log.error("domain error code={} path={}", ec.code(), pd.getProperties().get(ErrorKeys.PATH), ex);

        return ResponseEntity.status(ec.status()).body(pd);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ProblemDetail> handleBadInput(Exception ex, WebRequest req) {
        var err = CommonError.REQ_BAD_INPUT;
        var pd = pdf.base(err.status(), err.message(), readable(ex, err.message()), err.code(), req);

        log.warn("400 bad-input path={} msg={}", pd.getProperties().get(ErrorKeys.PATH), ex.getMessage());

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        var err = CommonError.REQ_VALIDATION;
        var pd  = pdf.base(err.status(), err.message(), "Request body contains invalid fields", err.code(), req);
        var errors = FieldErrorMapper.from(ex.getBindingResult(), props.maskFields());
        pd.setProperty(ErrorKeys.ERRORS, errors);

        log.warn("400 validation size={} path={}", errors.size(), pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, WebRequest req) {
        var err = CommonError.REQ_VALIDATION;
        var pd  = pdf.base(err.status(), err.message(), "Constraint violation", err.code(), req);
        var violations = ViolationMapper.from(ex, props.maskProps());
        pd.setProperty(ErrorKeys.ERRORS, violations);

        log.warn("400 constraint size={} path={}", violations.size(), pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ProblemDetail> handleIllegal(RuntimeException ex, WebRequest req) {
        var err = CommonError.REQ_BAD_INPUT;
        var pd = pdf.base(err.status(), err.message(), readable(ex, err.message()), err.code(), req);

        log.warn("400 illegal-arg path={} msg={}", pd.getProperties().get(ErrorKeys.PATH), ex.getMessage());

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, WebRequest req) {
        var err = CommonError.  NOT_FOUND;
        var pd = pdf.base(err.status(), err.message(), "Resource not found: " + ex.getResourcePath(), err.code(), req);

        log.warn("404 not-found path={}", pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest req) {
        String msg = ex.getMostSpecificCause().getMessage();

        boolean duplicate = false;
        Throwable cause = ex.getCause();

        if (cause instanceof org.hibernate.exception.ConstraintViolationException hce) {
            String sqlState = hce.getSQLState();
            int vendorCode  = hce.getErrorCode();
            duplicate = "23000".equals(sqlState) || vendorCode == 1062;
        } else if (msg != null && msg.toLowerCase().contains("duplicate")) {
            duplicate = true;
        }

        var err = duplicate ? CommonError.DATA_DUPLICATE : CommonError.DATA_INTEGRITY;
        var pd = pdf.base(err.status(), err.message(), "Data integrity violation", err.code(), req);

        if (msg != null && props.exposeDbReason()) {
            pd.setProperty(ErrorKeys.REASON, msg);
        }


        log.warn("db-integrity duplicate={} path={}", duplicate, pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleEtc(Exception ex, WebRequest req) {
        var err = CommonError.GEN_000;
        var pd = pdf.base(err.status(), err.message(), err.message(), err.code(), req);

        log.error("500 unhandled path={}", pd.getProperties().get(ErrorKeys.PATH), ex);

        return ResponseEntity.status(err.status()).body(pd);
    }

    private String readable(Exception ex, String fallback) {
        return ex.getMessage()!=null ? ex.getMessage() : fallback;
    }
}
