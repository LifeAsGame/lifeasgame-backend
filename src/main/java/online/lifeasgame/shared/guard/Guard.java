package online.lifeasgame.shared.guard;

import java.util.Objects;
import java.util.Set;

public final class Guard {
    private Guard() {}

    public static <T> T notNull(T v, String name) { return Objects.requireNonNull(v, name + " must not be null"); }

    public static String notBlank(String s, String name) {
        notNull(s, name);
        String v = s.strip(); // Java 11+, 유니코드 공백 대응
        if (v.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return v;
    }

    public static String maxLength(String s, int max, String name) {
        notNull(s, name);
        if (s.length() > max) throw new IllegalArgumentException(name + " length must be <= " + max);
        return s;
    }

    public static int inRange(int value, int min, int max, String name) {
        if (value < min || value > max)
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        return value;
    }

    public static <E> E oneOf(E value, Set<E> allowed, String name) {
        notNull(value, name);
        if (!allowed.contains(value)) throw new IllegalArgumentException(name + " must be one of " + allowed);
        return value;
    }

    public static void checkState(boolean ok, String msg) {
        if (!ok) throw new IllegalStateException(msg);
    }

    public static void check(boolean ok, String msg) { if (!ok) throw new IllegalStateException(msg); }
}
