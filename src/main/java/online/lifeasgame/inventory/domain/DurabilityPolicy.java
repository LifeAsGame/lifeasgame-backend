package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DurabilityPolicy {

    @Column(name = "max_durability")
    private Integer max;

    private DurabilityPolicy(Integer max) {
        this.max = Guard.minValue(Objects.requireNonNull(max), 1, "maxDurability");
    }

    public static DurabilityPolicy of(int max) {
        return new DurabilityPolicy(max);
    }

    public int max() {
        return max;
    }
}
