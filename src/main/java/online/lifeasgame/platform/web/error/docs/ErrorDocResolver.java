package online.lifeasgame.platform.web.error.docs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
public class ErrorDocResolver {
    private final ResourceLoader loader;

    private final String baseDir;
    private final String fallbackCode;

    public ErrorDocResolver(
            ResourceLoader loader,
            @Value("${app.error.docs.base:classpath:/static/errors}") String baseDir,
            @Value("${app.error.docs.fallback:NOT-FOUND}") String fallbackCode
    ) {
        this.loader = loader;
        this.baseDir = baseDir;
        this.fallbackCode = fallbackCode;
    }

    public boolean exists(String code) {
        return loader.getResource(baseDir + "/" + code + ".html").exists();
    }

    public String forwardTarget(String code) {
        return "/errors/" + code + ".html";
    }

    public String fallbackForward() {
        return forwardTarget(fallbackCode);
    }
}
