package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(name="equipment_slots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSlot extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, nullable = false, unique = true)
    private String code;

    @Column(length = 40, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EquipmentSlotCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EquipmentSlotRole role;

    private EquipmentSlot(
            String code,
            String name,
            EquipmentSlotCategory category,
            EquipmentSlotRole role
    ) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.role = role;
    }

    public static EquipmentSlot of(
            String code,
            String name,
            EquipmentSlotCategory category
    ) {
        return new EquipmentSlot(code, name, category, EquipmentSlotRole.SINGLE);
    }

    public static EquipmentSlot of(
            String code,
            String name,
            EquipmentSlotCategory category,
            EquipmentSlotRole role
    ) {
        return new EquipmentSlot(code, name, category, role);
    }
}
