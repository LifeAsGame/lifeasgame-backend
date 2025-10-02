package online.lifeasgame.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "items",
        indexes = @Index(name = "idx_item_name", columnList = "name")
)
public class Item extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ItemName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 40, nullable = false)
    private ItemType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Rarity rarity = Rarity.COMMON;

    @Convert(converter = BaseAttrsConverter.class)
    @Column(name = "base_attrs", columnDefinition = "json")
    private BaseAttrs baseAttrs = BaseAttrs.empty();

    @Column(name = "stackable", nullable = false)
    private boolean stackable = false;

    @Column(name = "max_stack", nullable = false)
    private int maxStack = 1;

    @Embedded
    private DurabilityPolicy durabilityPolicy;

    private Item(
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            int maxStack,
            DurabilityPolicy durabilityPolicy
    ) {
        this.name = Guard.notNull(name, "name");
        this.category = Guard.notNull(category, "category");
        this.type = Guard.notNull(type, "type");
        this.rarity = (rarity == null) ? Rarity.COMMON : rarity;
        this.baseAttrs = (baseAttrs == null) ? BaseAttrs.empty() : baseAttrs;
        this.stackable = stackable;
        this.maxStack = stackable ? Guard.minValue(maxStack, 2, "maxStack") : 1;
        this.durabilityPolicy = durabilityPolicy;
    }

    public static Item create(
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurabilityOrNull
    ) {
        int ms = (stackable) ? Guard.minValue(Optional.ofNullable(maxStack).orElse(0), 2, "maxStack") : 1;
        DurabilityPolicy dp = (maxDurabilityOrNull==null) ? null : DurabilityPolicy.of(maxDurabilityOrNull);
        return new Item(name, category, type, rarity, baseAttrs, stackable, ms, dp);
    }

    public void update(
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurabilityOrNull
    ) {
        this.name = Guard.notNull(name, "name");
        this.category = Guard.notNull(category, "category");
        this.type = Guard.notNull(type, "type");
        this.rarity = (rarity == null) ? Rarity.COMMON : rarity;
        this.baseAttrs = (baseAttrs == null) ? BaseAttrs.empty() : baseAttrs;
        this.stackable = stackable;
        this.maxStack = stackable ? Guard.minValue(Objects.requireNonNullElse(maxStack, 0), 2, "maxStack") : 1;
        this.durabilityPolicy = (maxDurabilityOrNull == null) ? null : DurabilityPolicy.of(maxDurabilityOrNull);
    }

    public boolean isStackable() {
        return stackable;
    }

    public int maxStack() {
        return maxStack;
    }

    public Optional<DurabilityPolicy> durabilityPolicy() {
        return Optional.ofNullable(durabilityPolicy);
    }
}
