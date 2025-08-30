package online.lifeasgame.platform.web.error.handler;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.ErrorKeys;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.core.tracing.MDCKeys;
import online.lifeasgame.platform.web.util.RequestUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
@RequiredArgsConstructor
public class ProblemDetailFactory {

    private final ErrorDocLinker linker;

    public ProblemDetail base(HttpStatus status, String title, String detail, String code, WebRequest req) {
        var pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);

        if (detail != null) pd.setDetail(detail);

        var path = RequestUtils.path(req);
        if (path != null) pd.setInstance(URI.create(RequestUtils.origin(req) + path));

        pd.setProperty(ErrorKeys.PATH, path);
        pd.setProperty(ErrorKeys.TRACE_ID, MDC.get(MDCKeys.TRACE_ID));
        pd.setProperty(ErrorKeys.CODE, code);

        linker.link(pd, code, req);

        return pd;
    }
}
