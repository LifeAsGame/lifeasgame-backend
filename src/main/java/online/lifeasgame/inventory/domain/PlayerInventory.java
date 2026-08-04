package online.lifeasgame.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.event.DomainEvent;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.inventory.domain.event.InventoryItemAdded;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.*;

@Getter
@Entity
@AggregateRoot
@Table(name = "player_inventory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerInventory extends AbstractTime {

    public static final int DEFAULT_CAPACITY = 60;

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots;

    @Version
    private Long version;

    @Getter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InventoryEntry> entries;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private PlayerInventory(Long playerId, int capacitySlots) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.capacitySlots = Guard.minValue(capacitySlots, 1, "capacitySlots");
        this.entries = new ArrayList<>();
    }

    public static PlayerInventory create(Long playerId) {
        return new PlayerInventory(playerId, DEFAULT_CAPACITY);
    }

    public static PlayerInventory of(Long playerId, int capacitySlots) {
        return new PlayerInventory(playerId, capacitySlots);
    }

    public Optional<InventoryEntry> findBySlot(SlotIndex slot) {
        return entries.stream()
                .filter(inventoryEntry -> inventoryEntry.slotIndex.equals(slot))
                .findFirst();
    }

    public boolean isSlotFree(SlotIndex slot) {
        return findBySlot(slot).isEmpty() && slot.value() >= 0 && slot.value() < capacitySlots;
    }

    private void ensureValidSlot(SlotIndex slot) {
        if (slot.value() < 0 || slot.value() >= capacitySlots) {
            throw new DomainException(InventoryError.INVALID_SLOT);
        }
    }

    public List<SlotIndex> add(ItemCarryPolicy itemCarryPolicy, int quantity, InstanceAttrs attrs, boolean bound) {
        InstanceAttrs instanceAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;
        assertCanAdd(itemCarryPolicy, quantity, instanceAttrs, bound);

        List<InventoryEntry> stacks = stacksOf(itemCarryPolicy, bound, instanceAttrs);

        int remaining = quantity;
        for (InventoryEntry s : stacks) {
            if (remaining == 0) {
                break;
            }

            int add = itemCarryPolicy.clampAddToLimit(s.getQuantity().value(), remaining);

            if (add > 0) {
                s.increaseQuantity(add, itemCarryPolicy);
                remaining -= add;
            }
        }

        List<SlotIndex> placed = new ArrayList<>();
        while (remaining > 0) {
            SlotIndex slotIndex = nextFreeSlot();
            int put = itemCarryPolicy.stackable() ? Math.min(itemCarryPolicy.maxStack(), remaining) : 1;

            var e = InventoryEntry.of(
                    this,
                    slotIndex,
                    itemCarryPolicy,
                    Quantity.of(put),
                    itemCarryPolicy.maxDurability() == null ? null : Durability.of(itemCarryPolicy.maxDurability()),
                    bound,
                    instanceAttrs
            );

            entries.add(e);
            placed.add(slotIndex);
            remaining -= put;
        }

        recordEvent(
                InventoryItemAdded.of(
                    this.playerId,
                    itemCarryPolicy.itemId(),
                    itemCarryPolicy.rarity().name(),
                    itemCarryPolicy.stackable(),
                    bound,
                    quantity
                )
        );

        return placed;
    }

    public record Addition(
            ItemCarryPolicy policy,
            int quantity,
            InstanceAttrs attrs,
            boolean bound
    ) {
    }

    public void assertCanAdd(
            ItemCarryPolicy policy,
            int quantity,
            InstanceAttrs attrs,
            boolean bound
    ) {
        assertCanAddAll(List.of(new Addition(policy, quantity, attrs, bound)));
    }

    public void assertCanAddAll(List<Addition> additions) {
        Objects.requireNonNull(additions, "additions must not be null");
        List<SimulatedStack> simulated = entries.stream()
                .map(entry -> new SimulatedStack(
                        entry.getItemId(),
                        entry.isBound(),
                        safeAttrs(entry.getInstAttrs()),
                        entry.getQuantity().value()
                ))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        for (Addition addition : additions) {
            Objects.requireNonNull(addition, "addition must not be null");
            if (addition.quantity() < 1) {
                throw new DomainException(InventoryError.INVALID_QUANTITY);
            }

            ItemCarryPolicy policy = Objects.requireNonNull(
                    addition.policy(),
                    "policy must not be null"
            );
            Map<String, Object> attrs = safeAttrs(addition.attrs());
            int remaining = addition.quantity();

            for (int index = 0; index < simulated.size() && remaining > 0; index++) {
                SimulatedStack stack = simulated.get(index);
                if (!Objects.equals(stack.itemId(), policy.itemId())
                        || !policy.sameStackKey(
                        stack.bound(),
                        stack.attrs(),
                        addition.bound(),
                        attrs
                )) {
                    continue;
                }

                int added = policy.clampAddToLimit(
                        stack.quantity(),
                        remaining
                );
                if (added > 0) {
                    simulated.set(index, stack.withQuantity(
                            stack.quantity() + added
                    ));
                    remaining -= added;
                }
            }

            while (remaining > 0) {
                if (simulated.size() >= capacitySlots) {
                    throw new DomainException(InventoryError.INVENTORY_FULL);
                }
                int put = policy.stackable()
                        ? Math.min(policy.maxStack(), remaining)
                        : 1;
                simulated.add(new SimulatedStack(
                        policy.itemId(),
                        addition.bound(),
                        attrs,
                        put
                ));
                remaining -= put;
            }
        }
    }

    private record SimulatedStack(
            Long itemId,
            boolean bound,
            Map<String, Object> attrs,
            int quantity
    ) {
        private SimulatedStack withQuantity(int nextQuantity) {
            return new SimulatedStack(itemId, bound, attrs, nextQuantity);
        }
    }

    /**
     * 제거 (0 되면 슬롯 비움)
     */
    public void remove(SlotIndex slot, int quantity) {
        ensureValidSlot(slot);

        InventoryEntry inventoryEntry = findBySlot(slot)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
        inventoryEntry.decreaseQuantity(quantity);

        if (inventoryEntry.getQuantity().value() == 0) {
            entries.remove(inventoryEntry);
        }
    }

    public void moveWithin(SlotIndex from, SlotIndex to) {
        ensureValidSlot(from);
        ensureValidSlot(to);

        InventoryEntry inventoryEntry = findBySlot(from)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        if (!isSlotFree(to)) {
            throw new DomainException(InventoryError.MOVE_CONFLICT);
        }

        inventoryEntry.changeSlot(to);
    }

    /**
     * 병합 (스택형 + 동일 스택 키)
     */
    public void merge(SlotIndex from, SlotIndex to, ItemCarryPolicy policy) {
        ensureValidSlot(from);
        ensureValidSlot(to);

        if (from.equals(to)) {
            return;
        }

        InventoryEntry fromEntry = findBySlot(from)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
        InventoryEntry toEntry = findBySlot(to)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        if (!fromEntry.canMergeWith(toEntry, policy)) {
            throw new DomainException(InventoryError.MERGE_NOT_COMPATIBLE);
        }

        int space = policy.spaceInStack(toEntry.getQuantity().value());
        if (space <= 0) {
            return;
        }

        int moved = Math.min(space, fromEntry.getQuantity().value());
        fromEntry.decreaseQuantity(moved);
        toEntry.increaseQuantity(moved, policy);

        if (fromEntry.getQuantity().value() == 0) {
            entries.remove(fromEntry);
        }
    }

    public SlotIndex split(SlotIndex from, SlotIndex toOpt, int quantity, ItemCarryPolicy policy) {
        ensureValidSlot(from);

        InventoryEntry inventoryEntry = findBySlot(from)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        SlotIndex to = (toOpt != null) ? toOpt : nextFreeSlot();
        if (!isSlotFree(to)) {
            throw new DomainException(InventoryError.SLOT_OCCUPIED);
        }

        InventoryEntry splitedInventoryEntry = inventoryEntry.splitTo(this, to, quantity, policy);
        entries.add(splitedInventoryEntry);

        if (inventoryEntry.getQuantity().value() == 0) {
            entries.remove(inventoryEntry);
        }

        return to;
    }

    private SlotIndex nextFreeSlot() {
        for (int i = 0; i < capacitySlots; i++) {
            SlotIndex s = SlotIndex.of(i);
            if (isSlotFree(s)) {
                return s;
            }
        }

        throw new DomainException(InventoryError.INVENTORY_FULL);
    }

    public List<InventoryEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    private List<InventoryEntry> stacksOf(ItemCarryPolicy policy, boolean bound, InstanceAttrs attrs) {
        Map<String, Object> target = (attrs == null) ? Map.of() : attrs.attrs();
        return entries.stream()
                .filter(
                        inventoryEntry -> Objects.equals(inventoryEntry.getItemId(), policy.itemId())
                                && policy.sameStackKey(
                                inventoryEntry.isBound(),
                                safeAttrs(inventoryEntry.getInstAttrs()),
                                bound,
                                target
                        )
                )
                .toList();
    }

    private Map<String, Object> safeAttrs(InstanceAttrs x) {
        return (x == null) ? Map.of() : x.attrs();
    }

    public InventoryEntry getEntry(SlotIndex of) {
        return entries.stream()
                .filter(e -> e.slotIndex.equals(of))
                .findFirst()
                .orElse(null);
    }

    public List<DomainEvent> pullEvents() {
        var copy = List.copyOf(domainEvents);
        domainEvents.clear();
        return copy;
    }

    private void recordEvent(DomainEvent event) {
        if (event == null) {
            return;
        }
        domainEvents.add(event);
    }
}
