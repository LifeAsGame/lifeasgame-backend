package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Durability {

    @Column(name = "durability")
    private Integer value;

    private Durability(Integer v) {
        this.value = Guard.minValue(v, 0, "durability");
    }

    public static Durability of(Integer v) {
        if (v == null) {
            return null;
        }
        return new Durability(v);
    }

    public Integer value() {
        return value;
    }
}
