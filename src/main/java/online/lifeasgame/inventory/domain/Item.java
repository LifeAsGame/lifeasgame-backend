package online.lifeasgame.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.Objects;
import java.util.Optional;

@Getter
@Entity
@AggregateRoot
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "items", indexes = @Index(name = "idx_item_name", columnList = "name")
)
public class Item extends AbstractTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private ItemCode code;

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
            ItemCode code,
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            int maxStack,
            DurabilityPolicy durabilityPolicy
    ) {
        this.code = code;
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
        DurabilityPolicy durabilityPolicy = (maxDurabilityOrNull == null) ? null : DurabilityPolicy.of(maxDurabilityOrNull);
        return new Item(null, name, category, type, rarity, baseAttrs, stackable, ms, durabilityPolicy);
    }

    public static Item createContentItem(
            ItemCode code,
            ItemName name,
            ItemCategory category,
            ItemType type,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurabilityOrNull
    ) {
        ItemCode requiredCode = Guard.notNull(code, "code");
        int ms = (stackable) ? Guard.minValue(Optional.ofNullable(maxStack).orElse(0), 2, "maxStack") : 1;
        DurabilityPolicy durabilityPolicy = (maxDurabilityOrNull == null)
                ? null
                : DurabilityPolicy.of(maxDurabilityOrNull);
        return new Item(
                requiredCode,
                name,
                category,
                type,
                rarity,
                baseAttrs,
                stackable,
                ms,
                durabilityPolicy
        );
    }

    public void update(
            ItemName itemName,
            ItemCategory itemCategory,
            ItemType itemType,
            Rarity rarity,
            BaseAttrs baseAttrs,
            boolean stackable,
            Integer maxStack,
            Integer maxDurabilityOrNull
    ) {
        this.name = Guard.notNull(itemName, "name");
        this.category = Guard.notNull(itemCategory, "category");
        this.type = Guard.notNull(itemType, "type");
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

    public Integer maxDurability() {
        return (this.durabilityPolicy == null) ? null : this.durabilityPolicy.max();
    }
}
