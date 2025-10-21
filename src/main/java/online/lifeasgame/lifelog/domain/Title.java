package online.lifeasgame.lifelog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Title {

    @Column(name = "title_value", nullable = false)
    private String value;

    @Column(name = "title_original")
    private String original;

    private Title(String value, String original) {
        Guard.hasText(value, "title");
        this.value = value.trim();
        this.original = (original == null || original.isBlank()) ? null : original.trim();
    }

    public static Title of(String value) {
        return new Title(value, null);
    }

    public static Title of(String value, String original) {
        return new Title(value, original);
    }

    public String value() {
        return value;
    }

    public String original() {
        return original;
    }
}
