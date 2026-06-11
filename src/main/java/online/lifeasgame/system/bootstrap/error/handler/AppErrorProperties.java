package online.lifeasgame.system.bootstrap.error.handler;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.error")
public record AppErrorProperties(
        List<String> maskFields,
        List<String> maskProps,
        boolean exposeDbReason,
        boolean includeDetailInResponse,
        boolean maskDetailAlwaysInLogs
        ) {
    public AppErrorProperties {
        maskFields = (maskFields == null) ? List.of() : List.copyOf(maskFields);
        maskProps  = (maskProps  == null) ? List.of() : List.copyOf(maskProps);
    }
}
