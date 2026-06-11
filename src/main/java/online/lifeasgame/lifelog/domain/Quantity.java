package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quantity {

    @Column(name = "quantity_value", nullable = false)
    private Integer value;

    private Quantity(Integer value) {
        Guard.notNull(value, "quantity");
        Guard.minValue(value, 1, "quantity");
        this.value = value;
    }

    public static Quantity of(Integer value) {
        return new Quantity(value);
    }

    public Integer value() {
        return value;
    }
}
