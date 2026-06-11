package online.lifeasgame.support;

import online.lifeasgame.platform.web.error.GlobalExceptionHandler;
import online.lifeasgame.platform.web.error.PiiScrubber;
import online.lifeasgame.platform.web.error.docs.ErrorDocLinker;
import online.lifeasgame.platform.web.error.handler.ProblemDetailFactory;
import online.lifeasgame.system.bootstrap.error.handler.AppErrorProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WebMvcTestConfig {

    @Bean
    ProblemDetailFactory problemDetailFactory(ErrorDocLinker linker) {
        return new ProblemDetailFactory(linker);
    }

    @Bean
    GlobalExceptionHandler globalExceptionHandler(
            ProblemDetailFactory pdf,
            AppErrorProperties props,
            PiiScrubber scrubber
    ) {
        return new GlobalExceptionHandler(pdf, props, scrubber);
    }

    @Bean
    PiiScrubber piiScrubber() {
        return new PiiScrubber();
    }
}
