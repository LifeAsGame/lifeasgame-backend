package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemCategory {

    @Column(name = "category", length = 20, nullable = false)
    private String value;

    private ItemCategory(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase();
        Guard.notBlank(v, "item category");
        Guard.maxLength(v, 20, "item category");
        this.value = v;
    }

    public static ItemCategory of(String raw) {
        return new ItemCategory(raw);
    }

    public String value() {
        return value;
    }
}
