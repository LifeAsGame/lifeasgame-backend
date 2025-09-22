package online.lifeasgame.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import online.lifeasgame.core.annotation.AggregateRoot;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;

@Getter
@Entity
@AggregateRoot
@Table(name = "player_mailbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerMailbox {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots = 100; // 필요에 맞게

    @Version
    private Long version;

    @OneToMany(mappedBy = "mailbox", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MailboxEntry> entries = new ArrayList<>();

    private PlayerMailbox(Long playerId, int capacitySlots) {
        this.playerId = Guard.notNull(playerId, "playerId");
        this.capacitySlots = Guard.minValue(capacitySlots, 1, "capacitySlots");
    }

    public static PlayerMailbox of(Long playerId, int capacitySlots) {
        return new PlayerMailbox(playerId, capacitySlots);
    }

    /* ---- 조회/유틸 ---- */
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
            SlotIndex s = SlotIndex.of(i);
            if (isSlotFree(s)) return s;
        }
        throw new DomainException(InventoryError.MAILBOX_FULL);
    }

    /** 시스템/이벤트로 우편 지급 */
    public SlotIndex deliver(Item def, int quantity, InstanceAttrs attrs, boolean bound) {
        Guard.minValue(quantity, 1, "qty");
        InstanceAttrs safeAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;

        if (!def.isStackable()) {
            if (freeSlots() < quantity) {
                throw new DomainException(InventoryError.MAILBOX_FULL);
            }

            SlotIndex first = null;

            for (int i = 0; i < quantity; i++) {
                SlotIndex free = nextFreeSlot();

                MailboxEntry e = MailboxEntry.of(
                        this,
                        free,
                        def,
                        Quantity.of(1),
                        null, bound,
                        safeAttrs
                );

                def.durabilityPolicy().ifPresent(e::ensureDurabilityWithin);

                entries.add(e);

                if (first == null) {
                    first = free;
                }
            }

            return first;
        }

        int remaining = quantity;

        List<MailboxEntry> stacks = entries.stream()
                .filter(e -> e.isSameStackKey(def, bound, safeAttrs))
                .toList();

        for (MailboxEntry stack : stacks) {
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

        SlotIndex first = null;

        while (remaining > 0) {
            if (freeSlots() <= 0) {
                throw new DomainException(InventoryError.MAILBOX_FULL);
            }

            int put = Math.min(def.maxStack(), remaining);

            SlotIndex free = nextFreeSlot();

            MailboxEntry e = MailboxEntry.of(
                    this,
                    free,
                    def,
                    Quantity.of(put),
                    null,
                    bound,
                    safeAttrs
            );

            entries.add(e);

            if (first == null) {
                first = free;
            }

            remaining -= put;
        }

        return first;
    }

    /** 우편 수령: 메일함 슬롯에서 꺼내 인벤토리로 넣기 */
    public void claimToInventory(SlotIndex from, int qty, Item def, PlayerInventory targetInventory) {
        ensureValidSlot(from);
        MailboxEntry src = findBySlot(from).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        Guard.minValue(qty, 1, "qty");
        if (qty > src.quantity.value()) {
            throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);
        }

        // 인벤토리에 먼저 add(실패 시 메일 감소 방지)
        targetInventory.add(def, qty, src.instAttrs, src.bound);

        // 성공했으면 메일에서 차감/삭제
        src.decreaseQuantity(qty);

        if (src.quantity.value() == 0) {
            entries.remove(src);
        }
    }

    /** 전체 수령(가능한 만큼) — 필요 시 사용 */
    public void claimAllToInventory(PlayerInventory targetInventory, Item def) {
        // 같은 정의만 일괄 수령하고 싶을 때(확장 가능)
        List<MailboxEntry> list = new ArrayList<>(entries);

        for (MailboxEntry e : list) {
            int qty = e.quantity.value();
            targetInventory.add(def, qty, e.instAttrs, e.bound);
            entries.remove(e);
        }
    }
}
