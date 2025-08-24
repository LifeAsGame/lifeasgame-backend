package online.lifeasgame.skill.domain;

import java.util.Map;

public record BaseEffect(Map<String, Integer> stats) {
    public static BaseEffect of(Map<String,Integer> m){
        return new BaseEffect(m == null ? Map.of() : Map.copyOf(m));
    }
}
