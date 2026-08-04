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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Getter
@Entity
@AggregateRoot
@Table(name = "player_mailbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerMailbox extends AbstractTime {

    public static final int DEFAULT_CAPACITY = 100;

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots = DEFAULT_CAPACITY;

    @Version
    private Long version;

    @Getter(AccessLevel.NONE)
    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MailboxEntry> entries = new ArrayList<>();

    private PlayerMailbox(Long playerId, int capacitySlots) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.capacitySlots = Guard.minValue(capacitySlots, 1, "capacitySlots");
    }

    public static PlayerMailbox of(Long playerId, int capacitySlots) {
        return new PlayerMailbox(playerId, capacitySlots);
    }

    public Optional<MailboxEntry> findBySlot(SlotIndex slot) {
        return entries.stream().filter(e -> e.slotIndex.equals(slot)).findFirst();
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

    private SlotIndex nextFreeSlot() {
        for (int i = 0; i < capacitySlots; i++) {
            SlotIndex slotIndex = SlotIndex.of(i);
            if (isSlotFree(slotIndex)) {
                return slotIndex;
            }
        }
        throw new DomainException(InventoryError.MAILBOX_FULL);
    }

    public SlotIndex deliver(ItemCarryPolicy itemCarryPolicy, int quantity, InstanceAttrs attrs, boolean bound) {
        Guard.minValue(quantity, 1, "qty");
        InstanceAttrs safeAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;
        List<MailboxEntry> stacks = entries.stream()
                .filter(e -> e.isSameStackKey(itemCarryPolicy, bound, safeAttrs))
                .toList();

        int capacityInExisting = stacks.stream()
                .mapToInt(stack -> itemCarryPolicy.spaceInStack(stack.getQuantity().value()))
                .sum();
        int remainingAfterExisting = Math.max(0, quantity - capacityInExisting);
        int neededNew = itemCarryPolicy.estimateNewStacksNeeded(remainingAfterExisting);
        if (neededNew > freeSlots()) {
            throw new DomainException(InventoryError.MAILBOX_FULL);
        }

        int remaining = quantity;
        SlotIndex firstExisting = null; // 기존 스택 중 처음으로 건드린 슬롯
        for (MailboxEntry stack : stacks) {
            if (remaining == 0) {
                break;
            }

            int canPush = itemCarryPolicy.clampAddToLimit(stack.getQuantity().value(), remaining);
            if (canPush > 0) {
                stack.increaseQuantity(canPush, itemCarryPolicy);
                remaining -= canPush;
                if (firstExisting == null) {
                    firstExisting = stack.getSlotIndex();
                }
            }
        }

        SlotIndex firstNew = null;
        while (remaining > 0) {
            int put = itemCarryPolicy.stackable() ? Math.min(itemCarryPolicy.maxStack(), remaining) : 1;
            SlotIndex free = nextFreeSlot();
            MailboxEntry e = MailboxEntry.of(
                    this,
                    free,
                    itemCarryPolicy,
                    Quantity.of(put),
                    (itemCarryPolicy.maxDurability() == null ? null : Durability.of(itemCarryPolicy.maxDurability())),
                    bound,
                    safeAttrs
            );

            entries.add(e);

            if (firstNew == null) {
                firstNew = free;
            }

            remaining -= put;
        }

        // 반환 우선순위: 새로 만든 슬롯 > 기존에 채운 슬롯
        if (firstNew != null) return firstNew;
        if (firstExisting != null) return firstExisting;

        // quantity >= 1 이었으므로 이 지점 도달하면 로직 결함
        throw new IllegalStateException("deliver() must return a slot when quantity > 0");
    }

    public MailboxEntry getEntryOrThrow(SlotIndex slot) {
        ensureValidSlot(slot);
        return findBySlot(slot)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));
    }

    public void deleteEntry(SlotIndex slot) {
        ensureValidSlot(slot);

        MailboxEntry entry = findBySlot(slot)
                .orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        entries.remove(entry);
    }

    public record ClaimPlan(
            SlotIndex slotIndex,
            Long itemId,
            int quantity,
            InstanceAttrs attrs,
            boolean bound,
            ItemCarryPolicy policy
    ) {
    }

    public ClaimPlan planClaim(
            SlotIndex slot,
            int quantity,
            ItemCarryPolicy policy
    ) {
        MailboxEntry entry = getEntryOrThrow(slot);
        if (quantity < 1) {
            throw new DomainException(InventoryError.INVALID_QUANTITY);
        }
        if (quantity > entry.getQuantity().value()) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }
        if (!policy.itemId().equals(entry.getItemId())) {
            throw new DomainException(InventoryError.ITEM_NOT_FOUND);
        }
        return new ClaimPlan(
                slot,
                entry.getItemId(),
                quantity,
                entry.getInstAttrs(),
                entry.isBound(),
                policy
        );
    }

    public void applyClaim(ClaimPlan plan) {
        MailboxEntry entry = getEntryOrThrow(plan.slotIndex());
        if (!Objects.equals(entry.getItemId(), plan.itemId())
                || entry.getQuantity().value() < plan.quantity()
                || !Objects.equals(entry.getInstAttrs(), plan.attrs())
                || entry.isBound() != plan.bound()) {
            throw new IllegalStateException(
                    "Mailbox claim state changed after preflight"
            );
        }

        entry.decreaseQuantity(plan.quantity());
        if (entry.getQuantity().value() == 0) {
            entries.remove(entry);
        }
    }

    public List<MailboxEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
