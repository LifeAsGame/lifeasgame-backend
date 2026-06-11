package online.lifeasgame.character.domain;

import java.util.Map;
import online.lifeasgame.core.guard.Guard;

public record ExtraStatsDelta(Map<String, Integer> values) {
    public ExtraStatsDelta {
        Guard.notNull(values, "delta");

        for (String k : values.keySet()) {
            Guard.notBlank(k, "delta key");
        }
        values = Map.copyOf(values);
    }
}
