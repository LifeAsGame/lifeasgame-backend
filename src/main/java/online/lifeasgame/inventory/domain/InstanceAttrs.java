package online.lifeasgame.inventory.domain;

import java.util.Map;

public record InstanceAttrs(Map<String, Object> attrs) {
    public InstanceAttrs {
        attrs = (attrs == null) ? Map.of() : Map.copyOf(attrs);
    }

    public static InstanceAttrs empty() {
        return new InstanceAttrs(Map.of());
    }
}
