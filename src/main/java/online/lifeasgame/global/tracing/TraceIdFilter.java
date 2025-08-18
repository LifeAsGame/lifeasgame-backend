package online.lifeasgame.global.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.shared.support.IdGenerator;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String ANONYMOUS = "-";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getServletPath();
        return p.startsWith("/actuator/health") || p.startsWith("/actuator/prometheus");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String incoming = req.getHeader(MDCKeys.TRACE_ID_HEADER);
        String traceId = isValidTraceId(incoming) ? incoming : IdGenerator.newTraceId();

        MDC.put(MDCKeys.TRACE_ID, traceId);
        MDC.put(MDCKeys.PATH, req.getRequestURI());
        MDC.put(MDCKeys.USER_ID, ANONYMOUS);
        MDC.put(MDCKeys.USER_ENTRY_POINT, ANONYMOUS);
/*
        if (internalAuth != null) {
            Long userId = internalAuth.findUserId();
            if (userId != null) MDC.put(MDCKeys.USER_ID, String.valueOf(userId));
            String ep = internalAuth.findUserEntryPoint();
            if (ep != null) MDC.put(MDCKeys.USER_ENTRY_POINT, ep);
        }
*/
        res.setHeader(MDCKeys.TRACE_ID_HEADER, traceId);

        long startNs = System.nanoTime();
        Throwable thrown = null;

        try {
            chain.doFilter(req, res);
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            MDC.put(MDCKeys.ELAPSED_TIME, String.valueOf(elapsedMs));

            int status = res.getStatus();
            if (thrown == null) {
                log.info("Request success status={}", status);
            } else {
                int effective = (status >= 400) ? status : 500;
                log.error("Request failed status={}", effective, thrown);
            }

            MDC.clear();
        }
    }

    private boolean isValidTraceId(String v) {
        if (v == null) return false;
        if (v.length() > 64) return false;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (!(c == '-' || c == '_' || Character.isLetterOrDigit(c))) {
                return false;
            }
        }
        return true;
    }
}
