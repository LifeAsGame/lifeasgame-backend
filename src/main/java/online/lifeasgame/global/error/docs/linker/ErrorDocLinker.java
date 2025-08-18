package online.lifeasgame.global.error.docs.linker;

import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.WebRequest;

public interface ErrorDocLinker {
    void link(ProblemDetail pd, String code, WebRequest req);
}
