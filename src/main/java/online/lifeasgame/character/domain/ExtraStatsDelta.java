package online.lifeasgame.character.domain;

import java.util.Map;
import online.lifeasgame.shared.guard.Guard;

public record ExtraStatsDelta(Map<String,Integer> values) {
    public ExtraStatsDelta {
        Guard.notNull(values, "delta");
    }
}
