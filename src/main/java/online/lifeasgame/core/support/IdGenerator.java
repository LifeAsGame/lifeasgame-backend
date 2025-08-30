package online.lifeasgame.core.support;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String newTraceId() {
        return UUID.randomUUID().toString();
    }
}
