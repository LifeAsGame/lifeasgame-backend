package online.lifeasgame.global.error.docs.linker;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import online.lifeasgame.global.error.docs.config.AppErrorDocsProperties;
import online.lifeasgame.global.web.RequestUtils;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

@Component
@RequiredArgsConstructor
public class DefaultErrorDocLinker implements ErrorDocLinker {
    private final AppErrorDocsProperties props;

    @Override
    public void link(ProblemDetail pd, String code, WebRequest req) {
        if (!props.isConfigured()) return;

        String base = props.getBase();
        if (!base.startsWith("http")) {
            var origin = RequestUtils.origin(req);
            base = origin + (base.startsWith("/") ? base : "/"+base);
        }
        String file = code + (props.getExt()==null ? "" : props.getExt());
        String uri = base.endsWith("/") ? base + file : base + "/" + file;

        pd.setType(URI.create(uri));
    }
}
