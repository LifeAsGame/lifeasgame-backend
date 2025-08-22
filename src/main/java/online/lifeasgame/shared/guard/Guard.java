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

    public static String minLength(String s, int min, String name) {
        notNull(s, name);
        if (min < 0) throw new IllegalArgumentException("min must be >= 0");
        if (s.length() < min) throw new IllegalArgumentException(name + " length must be " + ">= " + min );
        return s;
    }

    public static int maxValue(int value, int max, String name) {
        if (value > max) throw new IllegalArgumentException(name + " must be <= " + max);
        return value;
    }


    public static int minValue(int value, int min, String name) {
        if (value < min) throw new IllegalArgumentException(name + " must be >= " + min);
        return value;
    }

    public static long minValue(long value, long min, String name) {
        if (value < min) throw new IllegalArgumentException(name + " must be >= " + min);
        return value;
    }

    public static int inRange(int value, int min, int max, String name) {
        if (min > max) throw new IllegalArgumentException("min must be <= max");
        if (value < min || value > max)
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        return value;
    }

    public static <E> E oneOf(E value, Set<E> allowed, String name) {
        notNull(value, name);
        notNull(allowed, name + " allowed set");
        if (allowed.isEmpty()) throw new IllegalArgumentException(name + " allowed set must not be empty");
        if (!allowed.contains(value)) throw new IllegalArgumentException(name + " must be one of " + allowed);
        return value;
    }

    public static void checkState(boolean ok, String msg) {
        if (!ok) throw new IllegalStateException(msg);
    }

    public static void check(boolean ok, String msg) { if (!ok) throw new IllegalStateException(msg); }
}
