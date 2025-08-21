package online.lifeasgame.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Volume {

    public static final int MIN = 0;
    public static final int MAX = 100;

    @Column(name = "volume", nullable = false)
    private int volume;

    private Volume(int volume) {
        Guard.inRange(volume, MIN, MAX, "volume");
        this.volume = volume;
    }

    public static Volume of(int raw) {
        return new Volume(raw);
    }
}
