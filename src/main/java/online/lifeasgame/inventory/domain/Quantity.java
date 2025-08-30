package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quantity {
    @Column(name = "quantity", nullable = false)
    private int value;

    private Quantity(int v) {
        Guard.minValue(v, 0, "quantity");
        this.value = v;
    }

    public static Quantity of(int v) {
        return new Quantity(v);
    }

    public int value() {
        return value;
    }
}
