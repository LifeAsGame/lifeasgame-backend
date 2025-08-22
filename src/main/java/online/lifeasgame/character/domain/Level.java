package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Level {

    @Column(name = "level", nullable = false)
    private int value;

    private Level(int v) {
        Guard.minValue(v, 1, "level");
        this.value = v;
    }

    public static Level of(int v) {
        return new Level(v);
    }

    public int value() {
        return value;
    }

    public Level next() {
        return new Level(value + 1);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Level l) && l.value == value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }
}
