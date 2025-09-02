package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Volume {

    public static final int MIN = 0;
    public static final int MAX = 100;

    @Column(name = "volume", nullable = false)
    private int value;

    private Volume(int value) {
        Guard.inRange(value, MIN, MAX, "volume");
        this.value = value;
    }

    public static Volume of(int value) {
        return new Volume(value);
    }
}
