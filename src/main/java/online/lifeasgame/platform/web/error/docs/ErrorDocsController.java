package online.lifeasgame.platform.web.error.docs;

import jakarta.servlet.http.HttpServletResponse;
import online.lifeasgame.platform.web.error.docs.util.ErrorCodeSanitizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/errors")
public class ErrorDocsController {

    private final ErrorDocResolver resolver;
    public ErrorDocsController(ErrorDocResolver resolver) { this.resolver = resolver; }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ModelAndView show(@PathVariable String code, HttpServletResponse res) {
        code = ErrorCodeSanitizer.sanitize(code);

        if (!ErrorCodeSanitizer.isSafe(code) || !resolver.exists(code)) {
            res.setStatus(HttpStatus.NOT_FOUND.value());
            return new ModelAndView("forward:" + resolver.fallbackForward());
        }

        return new ModelAndView("forward:" + resolver.forwardTarget(code));
    }
}
