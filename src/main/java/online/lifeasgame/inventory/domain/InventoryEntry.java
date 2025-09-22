package online.lifeasgame.inventory.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;

@Entity
@Table(
        name = "inventory_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_inventory_slot", columnNames = {"player_id", "slot_index"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerInventory inventory;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "slot_index", nullable = false)
    )
    SlotIndex slotIndex;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Rarity rarity = Rarity.COMMON;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    Quantity quantity;

    @Embedded
    private Durability durability; // optional

    @Column(nullable = false)
    private boolean bound = false;

    @Convert(converter = InstanceAttrsConverter.class)
    @Column(name = "inst_attrs", columnDefinition = "json")
    private InstanceAttrs instAttrs = InstanceAttrs.empty();

    private InventoryEntry(
            PlayerInventory inv,
            SlotIndex slotIndex,
            Item item,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs instAttrs
    ) {
        this.inventory = Guard.notNull(inv, "inventory");
        this.slotIndex = Guard.notNull(slotIndex, "slotIndex");
        this.itemId = Guard.notNull(item.getId(), "itemId");
        this.rarity = item.getRarity();
        this.quantity = Guard.notNull(quantity, "quantity");
        this.bound = bound;
        this.instAttrs = (instAttrs == null) ? InstanceAttrs.empty() : instAttrs;

        ensureStackRule(item, quantity.value());
        this.durability = applyDurability(item, durability);
    }

    static InventoryEntry of(
            PlayerInventory inv,
            SlotIndex slotIndex,
            Item item,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs instAttrs
    ) {
        return new InventoryEntry(inv, slotIndex, item, quantity, durability, bound, instAttrs);
    }

    private void ensureStackRule(Item def, int q) {
        if (!def.isStackable() && q != 1) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        if (def.isStackable() && (q < 1 || q > def.maxStack())) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }
    }

    private Durability applyDurability(Item def, Durability given) {
        if (def.durabilityPolicy().isEmpty()) {
            return null;
        }

        DurabilityPolicy dp = def.durabilityPolicy().get();
        Durability d = (given == null) ? Durability.of(dp.max()) : given;

        if (d.value() < 0 || d.value() > dp.max()) {
            throw new DomainException(InventoryError.DURABILITY_POLICY);
        }

        return d;
    }

    void ensureDurabilityWithin(DurabilityPolicy dp) {
        if (durability == null) {
            durability = Durability.of(dp.max());
        }

        if (durability.value() < 0 || durability.value() > dp.max()) {
            throw new DomainException(InventoryError.DURABILITY_POLICY);
        }
    }

    public boolean isSameStackKey(Item def, boolean bound, InstanceAttrs attrs) {
        return Objects.equals(itemId, def.getId())
                && this.bound == bound
                && Objects.equals(
                        this.instAttrs == null ? Map.of() : this.instAttrs.attrs(),
                        attrs == null ? Map.of() : attrs.attrs()
                );
    }

    public boolean canMergeWith(InventoryEntry other, Item def) {
        if (!def.isStackable()) {
            return false;
        }

        if (!Objects.equals(itemId, other.itemId)) {
            return false;
        }

        if (bound != other.bound) {
            return false;
        }

        return Objects.equals(instAttrs, other.instAttrs);
    }

    public void increaseQuantity(int delta, Item def) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() + delta;

        if (!def.isStackable() || next > def.maxStack()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        this.quantity = Quantity.of(next);
    }

    public void decreaseQuantity(int delta) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() - delta;

        if (next < 0) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }

        this.quantity = Quantity.of(next);
    }

    public InventoryEntry splitTo(PlayerInventory inv, SlotIndex to, int qty, Item def) {
        if (!def.isStackable()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        if (qty <= 0 || qty >= quantity.value()) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }

        this.decreaseQuantity(qty);

        return InventoryEntry.of(
                inv,
                to,
                def,
                Quantity.of(qty),
                this.durability,
                this.bound,
                this.instAttrs
        );
    }

    public void changeSlot(SlotIndex to) {
        this.slotIndex = Guard.notNull(to, "slotIndex");
    }
}
