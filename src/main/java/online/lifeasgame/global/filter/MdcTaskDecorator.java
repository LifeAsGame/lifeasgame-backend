package online.lifeasgame.global.filter;

import java.util.Map;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {
    @NonNull
    @Override
    public Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
