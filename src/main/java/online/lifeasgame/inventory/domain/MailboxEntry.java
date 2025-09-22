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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;

@Getter
@Entity
@Table(
        name = "mailbox_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_mailbox_slot", columnNames = {"player_id", "slot_index"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailboxEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerMailbox mailbox;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "slot_index", nullable = false))
    SlotIndex slotIndex;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Rarity rarity = Rarity.COMMON;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "quantity", nullable = false)
    )
    Quantity quantity;

    @Embedded
    private Durability durability; // optional

    @Column(nullable = false)
    boolean bound = false;

    @Convert(converter = InstanceAttrsConverter.class)
    @Column(name = "inst_attrs", columnDefinition = "json")
    InstanceAttrs instAttrs = InstanceAttrs.empty();

    private MailboxEntry(PlayerMailbox box, SlotIndex slotIndex, Item def,
                         Quantity quantity, Durability durability, boolean bound, InstanceAttrs attrs) {
        this.mailbox = Guard.notNull(box, "mailbox");
        this.slotIndex = Guard.notNull(slotIndex, "slotIndex");
        this.itemId = Guard.notNull(def.getId(), "itemId");
        this.rarity = def.getRarity();
        this.quantity = Guard.notNull(quantity, "quantity");
        this.bound = bound;
        this.instAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;
        ensureStackRule(def, quantity.value());
        this.durability = applyDurability(def, durability);
    }

    static MailboxEntry of(PlayerMailbox box, SlotIndex slot, Item def,
                           Quantity qty, Durability dur, boolean bound, InstanceAttrs attrs) {
        return new MailboxEntry(box, slot, def, qty, dur, bound, attrs);
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

    boolean isSameStackKey(Item def, boolean bound, InstanceAttrs attrs) {
        return Objects.equals(itemId, def.getId())
                && this.bound == bound
                && Objects.equals(this.instAttrs, (attrs == null ? InstanceAttrs.empty() : attrs));
    }

    void increaseQuantity(int delta, Item def) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() + delta;
        if (!def.isStackable() || next > def.maxStack()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }
        this.quantity = Quantity.of(next);
    }

    void decreaseQuantity(int delta) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() - delta;
        if (next < 0) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }
        this.quantity = Quantity.of(next);
    }
}
