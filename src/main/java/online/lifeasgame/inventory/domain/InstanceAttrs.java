package online.lifeasgame.inventory.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceAttrs {

    @Transient
    private Map<String, Object> attrs;

    InstanceAttrs(Map<String, Object> attrs) {
        this.attrs = (attrs == null) ? Map.of() : Map.copyOf(attrs);
    }

    public static InstanceAttrs empty() {
        return new InstanceAttrs(Map.of());
    }

    public static InstanceAttrs of(Map<String, Object> stringObjectMap) {
        return new InstanceAttrs(stringObjectMap);
    }

    public Map<String, Object> attrs() {
        return attrs;
    }
}
