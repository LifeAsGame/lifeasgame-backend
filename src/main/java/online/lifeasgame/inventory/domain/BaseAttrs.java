package online.lifeasgame.inventory.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Map;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
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

    public Map<String, Integer> attrs() {
        return attrs;
    }
}
