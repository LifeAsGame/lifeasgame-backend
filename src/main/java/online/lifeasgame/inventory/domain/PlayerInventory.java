package online.lifeasgame.inventory.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

import java.util.*;

@Getter
@Entity
@AggregateRoot
@Table(name = "player_inventory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerInventory extends AbstractTime {

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

    private PlayerInventory(Long playerId, int capacitySlots) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.capacitySlots = Guard.minValue(capacitySlots, 1, "capacitySlots");
        this.entries = new ArrayList<>();
    }

    public static PlayerInventory create(Long playerId) {
        return new PlayerInventory(playerId, 60);
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

    private int freeSlots() {
        return capacitySlots - entries.size();
    }

    /**
     * 아이템 추가 (스택 규칙 준수)
     */
    public List<SlotIndex> add(ItemCarryPolicy policy, int quantity, InstanceAttrs attrs, boolean bound) {
        Guard.minValue(quantity, 1, "qty");
        InstanceAttrs instanceAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;

        List<InventoryEntry> stacks = stacksOf(policy, bound, instanceAttrs);

        int capacityInExisting = stacks.stream()
                .mapToInt(s -> policy.spaceInStack(s.getQuantity().value()))
                .sum();
        int remainingAfterExisting = Math.max(0, quantity - capacityInExisting);
        int neededNew = policy.estimateNewStacksNeeded(remainingAfterExisting);
        if (neededNew > freeSlots()) {
            throw new DomainException(InventoryError.INVENTORY_FULL);
        }

        int remaining = quantity;
        for (InventoryEntry s : stacks) {
            if (remaining == 0) {
                break;
            }

            int add = policy.clampAddToLimit(s.getQuantity().value(), remaining);

            if (add > 0) {
                s.increaseQuantity(add, policy);
                remaining -= add;
            }
        }

        List<SlotIndex> placed = new ArrayList<>();
        while (remaining > 0) {
            SlotIndex slotIndex = nextFreeSlot();
            int put = policy.stackable() ? Math.min(policy.maxStack(), remaining) : 1;

            var e = InventoryEntry.of(
                    this,
                    slotIndex,
                    policy,
                    Quantity.of(put),
                    Durability.of(policy.maxDurability()),
                    bound,
                    instanceAttrs
            );

            entries.add(e);
            placed.add(slotIndex);
            remaining -= put;
        }
        return placed;
    }

    /**
     * 제거 (0 되면 슬롯 비움)
     */
    public void remove(SlotIndex slot, int quantity) {
        ensureValidSlot(slot);

        InventoryEntry inventoryEntry = findBySlot(slot)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
        inventoryEntry.decreaseQuantity(quantity);

        if (inventoryEntry.quantity.value() == 0) {
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
                        inventoryEntry -> inventoryEntry.getItemId().equals(policy.itemId())
                        && policy.sameStackKey(
                                inventoryEntry.isBound(),
                                (inventoryEntry.getInstAttrs() == null ? null : inventoryEntry.getInstAttrs().attrs()),
                                bound,
                                target
                        )
                )
                .toList();
    }
}
