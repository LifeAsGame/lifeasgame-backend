package online.lifeasgame.skill.domain;

import java.util.Map;

public record BaseEffect(Map<String, Integer> stats) {
    public BaseEffect {
        stats = stats == null ? Map.of() : Map.copyOf(stats);
    }

    public static BaseEffect of(Map<String,Integer> m){
        return new BaseEffect(m);
    }

    public static BaseEffect empty() {
        return new BaseEffect(Map.of());
    }
}
