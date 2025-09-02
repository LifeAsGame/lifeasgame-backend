package online.lifeasgame.platform.web.error;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.core.error.Sensitivity;
import online.lifeasgame.core.error.api.CommonError;
import online.lifeasgame.core.error.ErrorKeys;
import online.lifeasgame.platform.web.error.handler.FieldErrorMapper;
import online.lifeasgame.platform.web.error.handler.ProblemDetailFactory;
import online.lifeasgame.platform.web.error.handler.ViolationMapper;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import online.lifeasgame.core.error.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
    private final PiiScrubber scrubber;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBase(BaseException ex, WebRequest req) {
        var ec = ex.getErrorCode();
        var status = HttpStatus.valueOf(ec.status());
        String responseDetail = null;

        if (props.includeDetailInResponse()) {
            responseDetail = scrubber.scrub(ex.detail(), ec.sensitivity());
            if (responseDetail == null) responseDetail = ec.message();
        }

        var pd = pdf.base(status, ec.message(), responseDetail, ec.code(), req);

        String logDetail = ex.detail();
        if (props.maskDetailAlwaysInLogs()) {
            logDetail = scrubber.scrub(logDetail, ec.sensitivity());
        }

        if (status.is4xxClientError()) {
            log.warn("domain-4xx code={} status={} path={} detail={}",
                    ec.code(), ec.status(), pd.getProperties().get(ErrorKeys.PATH), logDetail);
        } else {
            log.error("domain-5xx code={} status={} path={} detail={}",
                    ec.code(), ec.status(), pd.getProperties().get(ErrorKeys.PATH), logDetail, ex);
        }

        return ResponseEntity.status(ec.status()).body(pd);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ProblemDetail> handleBadInput(Exception ex, WebRequest req) {
        var err = CommonError.REQ_BAD_INPUT;
        var status = HttpStatus.valueOf(err.status());
        String detail = buildResponseDetail(ex, err.message(), Sensitivity.PII);
        var pd = pdf.base(status, err.message(), detail, err.code(), req);

        String logMsg = buildLogMessage(ex.getMessage(), Sensitivity.PII);
        log.warn("400 bad-input path={} msg={}", pd.getProperties().get(ErrorKeys.PATH), logMsg);

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        var err = CommonError.REQ_VALIDATION;
        var status = HttpStatus.valueOf(err.status());
        var pd  = pdf.base(status, err.message(), "Request body contains invalid fields", err.code(), req);
        var errors = FieldErrorMapper.from(ex.getBindingResult(), props.maskFields());
        pd.setProperty(ErrorKeys.ERRORS, errors);

        log.warn("400 validation size={} path={}", errors.size(), pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex, WebRequest req) {
        var err = CommonError.REQ_VALIDATION;
        var status = HttpStatus.valueOf(err.status());
        var pd  = pdf.base(status, err.message(), "Constraint violation", err.code(), req);
        var violations = ViolationMapper.from(ex, props.maskProps());
        pd.setProperty(ErrorKeys.ERRORS, violations);

        log.warn("400 constraint size={} path={}", violations.size(), pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public ResponseEntity<ProblemDetail> handleIllegal(RuntimeException ex, WebRequest req) {
        var err = CommonError.REQ_BAD_INPUT;
        var status = HttpStatus.valueOf(err.status());
        String detail = buildResponseDetail(ex, err.message(), Sensitivity.PII);
        var pd = pdf.base(status, err.message(), detail, err.code(), req);

        String logMsg = buildLogMessage(ex.getMessage(), Sensitivity.PII);
        log.warn("400 illegal-arg path={} msg={}", pd.getProperties().get(ErrorKeys.PATH), logMsg);

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex, WebRequest req) {
        var err = CommonError.NOT_FOUND;
        var status = HttpStatus.valueOf(err.status());
        var pd = pdf.base(status, err.message(), "Resource not found: " + ex.getResourcePath(), err.code(), req);

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
        var status = HttpStatus.valueOf(err.status());
        var pd = pdf.base(status, err.message(), "Data integrity violation", err.code(), req);

        if (msg != null && props.exposeDbReason()) {
            pd.setProperty(ErrorKeys.REASON, msg);
        }


        log.warn("db-integrity duplicate={} path={}", duplicate, pd.getProperties().get(ErrorKeys.PATH));

        return ResponseEntity.status(err.status()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleEtc(Exception ex, WebRequest req) {
        var err = CommonError.GEN_000;
        var status = HttpStatus.valueOf(err.status());
        var pd = pdf.base(status, err.message(), err.message(), err.code(), req);

        log.error("500 unhandled path={}", pd.getProperties().get(ErrorKeys.PATH), ex);

        return ResponseEntity.status(err.status()).body(pd);
    }

    private String readable(Exception ex, String fallback) {
        return ex.getMessage()!=null ? ex.getMessage() : fallback;
    }

    private String buildResponseDetail(Exception ex, String fallback, Sensitivity sen) {
        if (!props.includeDetailInResponse()) {
            return null;
        }
        String raw = readable(ex, fallback);
        return scrubber.scrub(raw, sen);
    }

    private String buildLogMessage(String raw, Sensitivity sen) {
        if (raw == null) {
            return null;
        }

        if (!props.maskDetailAlwaysInLogs()) {
            return raw;
        }

        return scrubber.scrub(raw, sen);
    }
}
