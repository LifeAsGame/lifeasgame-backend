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

@Entity
@Table(
        name = "inventory_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_inventory_slot", columnNames = {"player_id", "slot_index"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryEntry extends AbstractTime {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", length = 32, nullable = false)
    private InventoryAvailability availability = InventoryAvailability.FREE;

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
            PlayerInventory playerInventory,
            SlotIndex slotIndex,
            ItemCarryPolicy itemCarryPolicy,
            Quantity quantity,
            Durability durability,
            boolean bound,
            InstanceAttrs instAttrs
    ) {
        itemCarryPolicy.assertValidInitialQuantity(quantity.value());

        Integer normalizedDurability = itemCarryPolicy.normalizedDurability(durability == null ? null : durability.value());
        return new InventoryEntry(
                playerInventory, slotIndex,
                itemCarryPolicy.itemId(),
                itemCarryPolicy.rarity(),
                quantity,
                normalizedDurability == null ? null : Durability.of(normalizedDurability),
                bound,
                instAttrs
        );
    }

    public void increaseQuantity(int delta, ItemCarryPolicy itemCarryPolicy) {
        assertOrdinaryMutationAllowed();
        Guard.minValue(delta, 1, "delta");

        int space = itemCarryPolicy.spaceInStack(quantity.value());
        if (!itemCarryPolicy.stackable() || delta > space) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        int next;
        try {
            next = Math.addExact(quantity.value(), delta);
        } catch (ArithmeticException ex) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        if (next > itemCarryPolicy.maxStack()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        this.quantity = Quantity.of(next);
    }

    public void decreaseQuantity(int delta) {
        assertOrdinaryMutationAllowed();
        Guard.minValue(delta, 1, "delta");
        int next;
        try {
            next = Math.subtractExact(quantity.value(), delta);
        } catch (ArithmeticException ex) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }

        if (next < 0) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }

        this.quantity = Quantity.of(next);
    }

    public boolean canMergeWith(InventoryEntry toEntry, ItemCarryPolicy itemCarryPolicy) {
        if (!itemCarryPolicy.stackable()) {
            return false;
        }

        if (!Objects.equals(this.itemId, toEntry.itemId)) {
            return false;
        }

        return itemCarryPolicy.sameStackKey(
                this.bound,
                safeAttrs(this.instAttrs),
                toEntry.bound,
                safeAttrs(toEntry.instAttrs)
        );
    }

    private Map<String, Object> safeAttrs(InstanceAttrs x) {
        return (x == null) ? Map.of() : x.attrs();
    }

    public InventoryEntry splitTo(PlayerInventory playerInventory, SlotIndex to, int quantity, ItemCarryPolicy itemCarryPolicy) {
        assertOrdinaryMutationAllowed();
        if (!itemCarryPolicy.stackable()) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }

        if (quantity <= 0 || quantity >= this.quantity.value()) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }

        this.decreaseQuantity(quantity);

        return InventoryEntry.of(
                playerInventory,
                to,
                itemCarryPolicy,
                Quantity.of(quantity),
                this.durability,
                this.bound,
                this.instAttrs
        );
    }

    public void changeSlot(SlotIndex to) {
        assertOrdinaryMutationAllowed();
        this.slotIndex = Guard.notNull(to, "slotIndex");
    }

    public void listForMarket() {
        transition(InventoryAvailability.FREE, InventoryAvailability.LISTED);
    }

    public void reserveForTrade() {
        transition(
                InventoryAvailability.LISTED,
                InventoryAvailability.RESERVED_FOR_TRADE
        );
    }

    public void releaseTradeReservation() {
        transition(
                InventoryAvailability.RESERVED_FOR_TRADE,
                InventoryAvailability.LISTED
        );
    }

    public void releaseListing() {
        transition(InventoryAvailability.LISTED, InventoryAvailability.FREE);
    }

    public void beginTransfer() {
        transition(
                InventoryAvailability.RESERVED_FOR_TRADE,
                InventoryAvailability.TRANSFER_PROCESSING
        );
    }

    void markEquipped() {
        transition(InventoryAvailability.FREE, InventoryAvailability.EQUIPPED);
    }

    void releaseEquipped() {
        transition(InventoryAvailability.EQUIPPED, InventoryAvailability.FREE);
    }

    boolean isFreeForOrdinaryStacking() {
        return availability == InventoryAvailability.FREE;
    }

    void assertOrdinaryMutationAllowed() {
        if (availability != InventoryAvailability.FREE) {
            throw new DomainException(InventoryError.INVENTORY_ENTRY_UNAVAILABLE);
        }
    }

    void assertAvailability(InventoryAvailability expected) {
        if (availability != expected) {
            throw new DomainException(
                    InventoryError.INVALID_AVAILABILITY_TRANSITION
            );
        }
    }

    private void transition(
            InventoryAvailability expected,
            InventoryAvailability next
    ) {
        assertAvailability(expected);
        availability = next;
    }
}
