package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Experience {

    @Column(name = "exp", nullable = false)
    private long value;

    private Experience(long v) {
        Guard.minValue(v, 0, "exp");
        this.value = v;
    }

    public static Experience of(long v) {
        return new Experience(v);
    }

    public long value() {
        return value;
    }

    public Experience plus(long delta) {
        Guard.minValue(delta, 0, "delta");
        return new Experience(value + delta);
    }
}
