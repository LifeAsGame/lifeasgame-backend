package online.lifeasgame.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;

import java.util.Map;
import java.util.Objects;

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
            Long itemId,
            Rarity rarity,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs instAttrs
    ) {
        this.inventory = Guard.notNull(inv, "inventory");
        this.slotIndex = Guard.notNull(slotIndex, "slotIndex");
        this.itemId = Guard.notNull(itemId, "itemId");
        this.rarity = rarity;
        this.quantity = Guard.notNull(quantity, "quantity");
        this.bound = bound;
        this.instAttrs = (instAttrs == null) ? InstanceAttrs.empty() : instAttrs;
        this.durability = durability;
    }

    static InventoryEntry of(
            PlayerInventory inv,
            SlotIndex slotIndex,
            ItemCarryPolicy p,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs instAttrs
    ) {
        p.assertValidInitialQuantity(quantity.value());
        Integer normalizedDurability = p.normalizedDurability(durability == null ? null : durability.value());
        return new InventoryEntry(
                inv, slotIndex,
                p.itemId(),
                p.rarity(),
                quantity,
                normalizedDurability == null ? null : Durability.of(normalizedDurability),
                bound,
                instAttrs
        );
    }

    public void increaseQuantity(int delta, ItemCarryPolicy p) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() + delta;
        if (!p.stackable() || next > p.maxStack()) {
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

    public boolean canMergeWith(InventoryEntry toEntry, ItemCarryPolicy policy) {
        if (!policy.stackable()) {
            return false;
        }

        if (!Objects.equals(itemId, toEntry.itemId)) {
            return false;
        }

        if (bound != toEntry.bound) {
            return false;
        }

        Map<String, Object> a = (instAttrs == null) ? Map.of() : instAttrs.attrs();
        Map<String, Object> b = (toEntry.instAttrs == null) ? Map.of() : toEntry.instAttrs.attrs();

        return Objects.equals(a, b);
    }

    public InventoryEntry splitTo(PlayerInventory playerInventory, SlotIndex to, int quantity, ItemCarryPolicy policy) {
        if (!policy.stackable()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        if (quantity <= 0 || quantity >= this.quantity.value()) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }

        this.decreaseQuantity(quantity);

        return InventoryEntry.of(
                playerInventory,
                to,
                policy,
                Quantity.of(quantity),
                this.durability,
                this.bound,
                this.instAttrs
        );
    }

    public void changeSlot(SlotIndex to) {
        this.slotIndex = Guard.notNull(to, "slotIndex");
    }
}
