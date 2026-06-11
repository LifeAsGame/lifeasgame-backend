package online.lifeasgame.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.Map;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "mailbox_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_mailbox_slot", columnNames = {"player_id", "slot_index"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailboxEntry extends AbstractTime {

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
            PlayerMailbox playerMailbox,
            SlotIndex slotIndex,
            ItemCarryPolicy itemCarryPolicy,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs attrs
    ) {
        this.mailbox = Guard.notNull(playerMailbox, "mailbox");
        this.slotIndex = Guard.notNull(slotIndex, "slotIndex");
        this.itemId = Guard.notNull(itemCarryPolicy.itemId(), "itemId");
        this.rarity = itemCarryPolicy.rarity();
        this.quantity = Guard.notNull(quantity, "quantity");
        this.bound = bound;
        this.instAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;

        itemCarryPolicy.assertValidInitialQuantity(quantity.value());
        Integer nd = itemCarryPolicy.normalizedDurability(durability == null ? null : durability.value());
        this.durability = (nd == null) ? null : Durability.of(nd);
    }

    static MailboxEntry of(
            PlayerMailbox playerMailbox,
            SlotIndex slotIndex,
            ItemCarryPolicy itemCarryPolicy,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs attrs
    ) {
        return new MailboxEntry(
                playerMailbox,
                slotIndex,
                itemCarryPolicy,
                quantity,
                durability,
                bound,
                attrs
        );
    }

    boolean isSameStackKey(ItemCarryPolicy itemCarryPolicy, boolean bound, InstanceAttrs attrs) {
        if (!Objects.equals(this.itemId, itemCarryPolicy.itemId())) {
            return false;
        }

        return itemCarryPolicy.sameStackKey(
                this.bound,
                safeAttrs(this.instAttrs),
                bound,
                safeAttrs(attrs)
        );
    }

    private Map<String, Object> safeAttrs(InstanceAttrs x) {
        return (x == null) ? Map.of() : x.attrs();
    }

    void increaseQuantity(int delta, ItemCarryPolicy p) {
        Guard.minValue(delta, 1, "delta");
        int next = quantity.value() + delta;
        if (!p.stackable() || next > p.maxStack()) {
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
