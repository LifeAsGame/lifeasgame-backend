package online.lifeasgame.core.tracing;

public final class MDCKeys {
    private MDCKeys() {}

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String TRACE_ID = "traceId";
    public static final String PATH = "path";
    public static final String USER_ID = "userId";
    public static final String USER_ENTRY_POINT = "userEntryPoint";
    public static final String ELAPSED_TIME = "elapsedTime";
}
