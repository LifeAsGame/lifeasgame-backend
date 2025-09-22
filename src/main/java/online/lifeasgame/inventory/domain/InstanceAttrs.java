package online.lifeasgame.inventory.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceAttrs {

    @Transient
    private Map<String, Object> attrs;

    public InstanceAttrs(Map<String, Object> attrs) {
        this.attrs = (attrs == null) ? Map.of() : Map.copyOf(attrs);
    }

    public static InstanceAttrs empty() {
        return new InstanceAttrs(Map.of());
    }

    public Map<String, Object> attrs() {
        return attrs;
    }
}
