package online.lifeasgame.inventory.domain;

import java.util.Map;

public record BaseAttrs(Map<String, Integer> attrs) {
    public BaseAttrs {
        attrs = (attrs == null) ? Map.of() : Map.copyOf(attrs);
    }

    public static BaseAttrs empty() {
        return new BaseAttrs(Map.of());
    }
}
