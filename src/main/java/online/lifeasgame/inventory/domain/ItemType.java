package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import online.lifeasgame.shared.guard.Guard;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemType {

    @Column(name = "type", length = 40, nullable = false)
    private String value;

    private ItemType(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase();
        Guard.notBlank(v, "item type");
        Guard.maxLength(v, 40, "item type");
        this.value = v;
    }

    public static ItemType of(String raw) {
        return new ItemType(raw);
    }

    public String value() {
        return value;
    }
}
