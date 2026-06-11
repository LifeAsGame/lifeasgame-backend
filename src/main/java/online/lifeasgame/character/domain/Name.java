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
public class Name {

    @Column(name = "name", length = 40, nullable = false)
    private String value;

    private Name(String raw) {
        String v = Guard.notBlank(raw, "name");
        Guard.inRange(v.length(), 1, 40, "name");
        this.value = v;
    }

    public static Name of(String raw) {
        return new Name(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
