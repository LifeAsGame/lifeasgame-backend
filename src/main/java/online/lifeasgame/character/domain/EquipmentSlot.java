package online.lifeasgame.character.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@Table(
        name = "equipment_slots",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_equipment_slot_code_version",
                columnNames = {"code", "definition_version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EquipmentSlot extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, nullable = false)
    private String code;

    @Column(length = 40, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EquipmentSlotCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EquipmentSlotRole role;

    @Column(name = "definition_version", length = 20, nullable = false)
    private String definitionVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "logical_category", length = 20)
    private EquipmentSlotLogicalCategory logicalCategory;

    @Column(name = "semantic_role", length = 500)
    private String semanticRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_tier", length = 2)
    private EquipmentSlotReleaseTier releaseTier;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", length = 20, nullable = false)
    private EquipmentSlotLifecycleStatus lifecycleStatus;

    @Column(name = "introduced_activation_wave", length = 40)
    private String introducedActivationWave;

    @Column(name = "replacement_slot_code", length = 40)
    private String replacementSlotCode;

    @Column(name = "source_revision", length = 200)
    private String sourceRevision;

    @Column(name = "approved_by", length = 200)
    private String approvedBy;

    @Column(name = "eager_on_link_start", nullable = false)
    private boolean eagerOnLinkStart;

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
        this.definitionVersion = "LEGACY";
        this.enabled = true;
        this.lifecycleStatus = EquipmentSlotLifecycleStatus.ACTIVE;
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

    public boolean supportsEquipmentCommand() {
        return enabled
                && lifecycleStatus == EquipmentSlotLifecycleStatus.ACTIVE
                && category != null
                && role != null;
    }

    public boolean isVisiblePlayerEquipmentSlot() {
        return enabled
                && lifecycleStatus == EquipmentSlotLifecycleStatus.ACTIVE
                && eagerOnLinkStart;
    }
}
