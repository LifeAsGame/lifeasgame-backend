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
import java.util.Optional;

@Getter
@Entity
@AggregateRoot
@Table(name = "player_mailbox")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerMailbox extends AbstractTime {

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "capacity_slots", nullable = false)
    private int capacitySlots = 100; // 필요에 맞게

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

    public SlotIndex deliver(ItemCarryPolicy p, int quantity, InstanceAttrs attrs, boolean bound) {
        Guard.minValue(quantity, 1, "qty");
        InstanceAttrs safeAttrs = (attrs == null) ? InstanceAttrs.empty() : attrs;

        int remaining = quantity;

        // 1) 기존 스택 채우기
        var stacks = entries.stream()
                .filter(e -> e.isSameStackKey(p, bound, safeAttrs))
                .toList();

        SlotIndex firstExisting = null; // 기존 스택 중 처음으로 건드린 슬롯
        for (MailboxEntry stack : stacks) {
            if (remaining == 0) break;
            int canPush = p.clampAddToLimit(stack.getQuantity().value(), remaining);
            if (canPush > 0) {
                stack.increaseQuantity(canPush, p);
                remaining -= canPush;
                if (firstExisting == null) {
                    firstExisting = stack.getSlotIndex();
                }
            }
        }

        // 2) Pre-flight: 새 스택 필요 개수 계산
        int capacityInExisting = stacks.stream()
                .mapToInt(s -> p.spaceInStack(s.getQuantity().value()))
                .sum();
        int remainAfterExisting = Math.max(0, remaining - capacityInExisting);
        int needNew = p.estimateNewStacksNeeded(remainAfterExisting);
        if (needNew > freeSlots()) {
            throw new DomainException(InventoryError.MAILBOX_FULL);
        }

        // 3) 신규 스택 생성
        SlotIndex firstNew = null;
        while (remaining > 0) {
            int put = p.stackable() ? Math.min(p.maxStack(), remaining) : 1;
            SlotIndex free = nextFreeSlot();
            MailboxEntry e = MailboxEntry.of(
                    this, free, p,
                    Quantity.of(put),
                    (p.maxDurability() == null ? null : Durability.of(p.maxDurability())),
                    bound, safeAttrs
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

    public MailboxEntry getEntry(SlotIndex of) {
        return entries.stream()
                .filter(e -> e.slotIndex.equals(of))
                .findFirst()
                .orElse(null);
    }

    /** 수령 슬라이스 VO (인벤토리에 넣을 정보) */
    public record ClaimedSlice(int quantity, InstanceAttrs attrs, boolean bound) {}

    /** 우편 수령: 메일함에서 차감만 하고, 인벤토리에 넣을 데이터를 반환 */
    public ClaimedSlice claim(SlotIndex from, int qty, ItemCarryPolicy p) {
        ensureValidSlot(from);
        MailboxEntry src = findBySlot(from).orElseThrow(() -> new DomainException(InventoryError.SLOT_EMPTY));

        Guard.minValue(qty, 1, "qty");
        if (qty > src.getQuantity().value()) throw new DomainException(InventoryError.NOT_ENOUGH_QUANTITY);

        // (정책 일관성 체크: 스택 규칙이 깨진 상태 방지용. 보통은 저장 당시 보장됨)
        if (!p.itemId().equals(src.getItemId())) throw new DomainException(InventoryError.ITEM_NOT_FOUND);

        // 먼저 메일함에서 차감
        src.decreaseQuantity(qty);
        if (src.getQuantity().value() == 0) entries.remove(src);

        // 인벤토리에 넣을 페이로드 반환
        return new ClaimedSlice(qty, src.getInstAttrs(), src.isBound());
    }

    public List<MailboxEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
