package online.lifeasgame.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import online.lifeasgame.global.constant.MDCKeys;
import online.lifeasgame.global.util.IdGenerator;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String MOCK_USER_ID = "12345";
    private static final String MOCK_USER_ENTRY_POINT = "WEB";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {

        String incoming = req.getHeader(MDCKeys.TRACE_ID_HEADER);
        String traceId = (incoming != null && !incoming.isBlank()) ? incoming : IdGenerator.newTraceId();

        MDC.put(MDCKeys.TRACE_ID, traceId);
        MDC.put(MDCKeys.PATH, req.getRequestURI());
        MDC.put(MDCKeys.USER_ID, MOCK_USER_ID);
        MDC.put(MDCKeys.USER_ENTRY_POINT, MOCK_USER_ENTRY_POINT);
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

            if (!MDCKeys.IGNORE_PATH.contains(req.getRequestURI())) {
                if (thrown == null) log.info("Request success status={}", res.getStatus());
                else log.error("Request failed status={}", res.getStatus(), thrown);
            }

            MDC.clear();
        }
    }
}