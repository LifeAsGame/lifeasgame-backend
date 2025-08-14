package online.lifeasgame.global.filter;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class MdcTaskDecorator implements TaskDecorator {
    @NonNull
    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    MDC.clear();
                }
                runnable.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
