package online.lifeasgame.shared.guard;

import java.util.Objects;

public final class Guard {
    private Guard() {}
    public static <T> T notNull(T v, String name) { return Objects.requireNonNull(v, name + " must not be null"); }
    public static void check(boolean ok, String msg) { if (!ok) throw new IllegalStateException(msg); }
}
