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

    private MailboxEntry(
            PlayerMailbox box, SlotIndex slot, ItemCarryPolicy p,
            Quantity qty, Durability dur, boolean bound, InstanceAttrs attrs
    ) {
        this.mailbox   = Guard.notNull(box, "mailbox");
        this.slotIndex = Guard.notNull(slot, "slotIndex");
        this.itemId    = Guard.notNull(p.itemId(), "itemId");
        this.rarity    = p.rarity();
        this.quantity  = Guard.notNull(qty, "quantity");
        this.bound     = bound;
        this.instAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;

        p.assertValidInitialQuantity(qty.value());
        Integer nd = p.normalizedDurability(dur == null ? null : dur.value());
        this.durability = (nd == null) ? null : Durability.of(nd);
    }

    static MailboxEntry of(
            PlayerMailbox box, SlotIndex slot, ItemCarryPolicy p,
            Quantity qty, Durability dur, boolean bound, InstanceAttrs attrs
    ) {
        return new MailboxEntry(box, slot, p, qty, dur, bound, attrs);
    }

    boolean isSameStackKey(ItemCarryPolicy p, boolean boundB, InstanceAttrs attrsB) {
        if (!Objects.equals(itemId, p.itemId())) return false;
        Map<String,Object> a = (instAttrs == null) ? Map.of() : instAttrs.attrs();
        Map<String,Object> b = (attrsB == null) ? Map.of() : attrsB.attrs();
        return p.sameStackKey(this.bound, a, boundB, b);
    }

    void increaseQuantity(int delta, ItemCarryPolicy p) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() + delta;
        if (!p.stackable() || next > p.maxStack()) throw new DomainException(InventoryError.INVALID_STACK_RULE);
        this.quantity = Quantity.of(next);
    }

    void decreaseQuantity(int delta) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() - delta;
        if (next < 0) throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        this.quantity = Quantity.of(next);
    }
}
