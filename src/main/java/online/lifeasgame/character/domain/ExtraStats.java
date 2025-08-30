package online.lifeasgame.character.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import online.lifeasgame.core.guard.Guard;

public final class ExtraStats {

    private static final Set<String> ALLOWED = Set.of(
            "sociability", "diligence", "creativity", "discipline", "leadership", "empathy"
    );
    private static final int MIN = 0;
    private static final int MAX_PER_STAT = 100;
    private static final int MAX_TOTAL = 500;

    private final Map<String,Integer> values;

    private ExtraStats(Map<String, Integer> normalized) {
        this.values = Collections.unmodifiableMap(normalized);
    }

    public static ExtraStats empty(){ return new ExtraStats(Map.of()); }

    public static ExtraStats of(Map<String, Integer> raw) {
        Guard.notNull(raw, "extraStats");

        Map<String, Integer> m = new HashMap<>();

        int sum = 0;

        for (var e : raw.entrySet()) {
            String key = normKey(e.getKey());

            if (!ALLOWED.contains(key)) {
                continue;
            }

            int v = clamp(e.getValue());
            m.put(key, v);
            sum += v;
        }

        Guard.maxValue(sum, MAX_TOTAL, "extraStats total");

        return new ExtraStats(m);
    }

    public ExtraStats apply(ExtraStatsDelta delta) {
        Guard.notNull(delta, "extraStatsDelta");

        Map<String, Integer> m = new HashMap<>(this.values);

        int sum = this.values.values().stream().mapToInt(i -> i).sum();

        for (var e : delta.values().entrySet()) {
            String key = normKey(e.getKey());

            if (!ALLOWED.contains(key)) {
                continue;
            }

            int base = m.getOrDefault(key, 0);
            int next = clamp(base + e.getValue());

            sum += (next - base);

            m.put(key, next);
        }

        Guard.maxValue(sum, MAX_TOTAL, "extraStats total");

        return new ExtraStats(m);
    }

    public Map<String, Integer> asMap() {
        return values;
    }

    private static String normKey(String k) {
        Guard.notNull(k, "key");
        return k.trim().toLowerCase(Locale.ROOT);
    }

    private static int clamp(Integer v) {
        Guard.notNull(v, "value");
        return Math.max(MIN, Math.min(MAX_PER_STAT, v));
    }
}
