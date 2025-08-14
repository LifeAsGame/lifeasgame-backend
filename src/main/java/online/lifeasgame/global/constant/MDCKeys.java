package online.lifeasgame.global.constant;

import java.util.Set;

public final class MDCKeys {
    private MDCKeys() {}

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String TRACE_ID = "traceId";
    public static final String PATH = "path";
    public static final String USER_ID = "userId";
    public static final String USER_ENTRY_POINT = "userEntryPoint";
    public static final String ELAPSED_TIME = "elapsedTime";

    public static Set<String> IGNORE_PATH = Set.of(
            "/actuator/prometheus",
            "/actuator/health"
    );
}
