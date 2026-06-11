package online.lifeasgame.inventory.domain;

import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BaseAttrs {

    @Transient
    private Map<String, Integer> attrs;

    public BaseAttrs(Map<String, Integer> attrs) {
        this.attrs = (attrs == null) ? Map.of() : Map.copyOf(attrs);
    }

    public static BaseAttrs empty() {
        return new BaseAttrs(Map.of());
    }

    public static BaseAttrs of(Map<String, Integer> baseAttrs) {
        return new BaseAttrs(baseAttrs);
    }

    public Map<String, Integer> attrs() {
        return attrs;
    }
}
