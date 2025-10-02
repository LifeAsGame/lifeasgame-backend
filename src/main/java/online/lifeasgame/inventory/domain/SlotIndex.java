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
public class SlotIndex {

    @Column(name = "slot_index", nullable = false)
    private int value;

    private SlotIndex(int v) {
        this.value = Guard.minValue(v, 0, "slot_index");
    }

    public static SlotIndex of(int v) {
        return new SlotIndex(v);
    }

    public static SlotIndex ofNullable(Integer v) {
        return (v == null) ? null : of(v);
    }

    public int value() {
        return value;
    }
}
