package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Durability {

    @Column(name = "durability")
    private Integer value;

    private Durability(Integer v) {
        this.value = v;
    }

    public static Durability of(int v) {
        return new Durability(v);
    }

    public Integer value() {
        return value;
    }
}
