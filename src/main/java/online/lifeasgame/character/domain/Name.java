package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Name {

    @Column(name = "name", length = 40, nullable = false)
    private String value;

    private Name(String raw) {
        String v = Guard.notBlank(raw, "name");
        Guard.inRange(v.length(), 0, 40, "name");
        this.value = v;
    }

    public static Name of(String raw) {
        return new Name(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Name n) && value.equals(n.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
