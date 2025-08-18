package online.lifeasgame.global.error.handler.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.error")
public record AppErrorProperties(
        List<String> maskFields,
        List<String> maskProps
) {
}
