package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Tag {

    @Column(name = "tag", length = 32, nullable = false)
    private String value;

    private Tag(String v) {
        String t = Guard.notBlank(v, "tag").trim();
        Guard.inRange(t.length(), 1, 32, "tag length");
        this.value = t.toLowerCase(Locale.ROOT);
    }

    public static Tag of(String v) {
        return new Tag(v);
    }

    public String value() {
        return value;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag tag)) return false;
        return Objects.equals(value, tag.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
