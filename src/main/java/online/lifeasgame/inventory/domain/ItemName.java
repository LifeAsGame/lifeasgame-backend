package online.lifeasgame.inventory.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemName {

    @Column(name = "name", length = 80, nullable = false, unique = true)
    private String value;

    private ItemName(String raw) {
        String v = raw == null ? "" : raw.trim();
        Guard.minLength(v, 2, "item name");
        Guard.maxLength(v, 10, "item name");
        this.value = v;
    }

    public static ItemName of(String raw) {
        return new ItemName(raw);
    }

    public String value() {
        return value;
    }
}
