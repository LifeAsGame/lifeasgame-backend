package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCode {

    public static final int MAX_LENGTH = 80;

    @Column(name = "code", length = MAX_LENGTH, unique = true)
    private String value;

    private ItemCode(String raw) {
        String normalized = Guard.notBlank(raw, "item code");
        this.value = Guard.maxLength(normalized, MAX_LENGTH, "item code");
    }

    public static ItemCode of(String raw) {
        return new ItemCode(raw);
    }

    public String value() {
        return value;
    }
}
