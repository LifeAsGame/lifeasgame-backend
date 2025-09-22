package online.lifeasgame.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;
import online.lifeasgame.platform.persistence.jpa.AbstractTime;

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
    private int capacitySlots = 60;

    @Column(name = "weight_limit")
    private Integer weightLimit;

    @Version
    private Long version;

    @Getter(AccessLevel.NONE)
    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InventoryEntry> entries = new ArrayList<>();

    private PlayerInventory(Long playerId, int capacitySlots, Integer weightLimit) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.capacitySlots = Guard.minValue(capacitySlots, 1, "capacitySlots");
        this.weightLimit = weightLimit;
    }

    public static PlayerInventory of(Long playerId, int capacitySlots) {
        return new PlayerInventory(playerId, capacitySlots, null);
    }

    public Optional<InventoryEntry> findBySlot(SlotIndex slot) {
        return entries.stream()
                .filter(e -> e.slotIndex.equals(slot))
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

    /** 아이템 추가 (스택 규칙 준수) */
    public List<SlotIndex> add(Item def, int quantity, InstanceAttrs attrs, boolean bound) {
        Guard.minValue(quantity, 1, "qty");
        InstanceAttrs safeAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;
        List<SlotIndex> placed = new ArrayList<>();

        if (!def.isStackable()) {
            if (freeSlots() < quantity) {
                throw new DomainException(InventoryError.INVENTORY_FULL);
            }

            for (int i = 0; i < quantity; i++) {
                SlotIndex free = nextFreeSlot();
                InventoryEntry e = InventoryEntry.of(this, free, def, Quantity.of(1), null, bound, safeAttrs);

                def.durabilityPolicy().ifPresent(e::ensureDurabilityWithin);

                entries.add(e);
                placed.add(free);
            }

            return placed;
        }

        int remaining = quantity;

        // 동일 스택 키(같은 정의/귀속/속성) 먼저 채우기
        List<InventoryEntry> stacks = entries.stream()
                .filter(e -> e.isSameStackKey(def, bound, safeAttrs))
                .toList();

        for (InventoryEntry stack : stacks) {
            if (remaining == 0) {
                break;
            }

            int canPush = def.maxStack() - stack.quantity.value();

            if (canPush <= 0) {
                continue;
            }

            int add = Math.min(canPush, remaining);
            stack.increaseQuantity(add, def);
            remaining -= add;
        }

        while (remaining > 0) {
            if (freeSlots() <= 0) {
                throw new DomainException(InventoryError.INVENTORY_FULL);
            }

            int put = Math.min(def.maxStack(), remaining);
            SlotIndex free = nextFreeSlot();
            InventoryEntry e = InventoryEntry.of(
                    this,
                    free,
                    def,
                    Quantity.of(put),
                    null,
                    bound,
                    safeAttrs
            );

            entries.add(e);
            placed.add(free);
            remaining -= put;
        }

        return placed;
    }

    /** 제거 (0 되면 슬롯 비움) */
    public void remove(SlotIndex slot, int qty) {
        ensureValidSlot(slot);

        InventoryEntry e = findBySlot(slot).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
        e.decreaseQuantity(qty);

        if (e.quantity.value() == 0) {
            entries.remove(e);
        }
    }

    /** 이동 (빈 슬롯만) */
    public void moveWithin(SlotIndex from, SlotIndex to) {
        ensureValidSlot(from);
        ensureValidSlot(to);

        InventoryEntry src = findBySlot(from).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        if (!isSlotFree(to)) {
            throw new DomainException(InventoryError.MOVE_CONFLICT);
        }

        src.changeSlot(to);
    }

    /** 병합 (스택형 + 동일 스택 키) */
    public void merge(SlotIndex from, SlotIndex to, Item def) {
        ensureValidSlot(from);
        ensureValidSlot(to);

        if (from.equals(to)) {
            return;
        }

        InventoryEntry a = findBySlot(from).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
        InventoryEntry b = findBySlot(to).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        if (!a.canMergeWith(b, def)) {
            throw new DomainException(InventoryError.MERGE_NOT_COMPATIBLE);
        }

        int space = def.maxStack() - b.quantity.value();

        if (space <= 0) {
            return;
        }

        int moved = Math.min(space, a.quantity.value());
        a.decreaseQuantity(moved);
        b.increaseQuantity(moved, def);

        if (a.quantity.value() == 0) {
            entries.remove(a);
        }
    }

    /** 분할 (from에서 qty를 to로) */
    public SlotIndex split(SlotIndex from, int qty, SlotIndex toOpt, Item def) {
        ensureValidSlot(from);

        InventoryEntry src = findBySlot(from).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        if (qty <= 0 || qty >= src.quantity.value()) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }

        SlotIndex to = (toOpt != null) ? toOpt : nextFreeSlot();

        if (!isSlotFree(to)) {
            throw new DomainException(InventoryError.SLOT_OCCUPIED);
        }

        InventoryEntry dst = src.splitTo(this, to, qty, def);
        entries.add(dst);

        if (src.quantity.value() == 0) {
            entries.remove(src);
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
}
