package online.lifeasgame.inventory.domain;

import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.core.guard.Guard;
import online.lifeasgame.inventory.domain.error.InventoryError;

import java.util.Map;
import java.util.Objects;

public record ItemCarryPolicy(
        Long itemId,
        Rarity rarity,
        boolean stackable,
        int maxStack,
        Integer maxDurability // nullable
) {

    public static ItemCarryPolicy from(Item item) {
        Guard.notNull(item, "item");
        return new ItemCarryPolicy(
                item.getId(),
                item.getRarity(),
                item.isStackable(),
                item.maxStack(),
                item.durabilityPolicy()
                        .map(DurabilityPolicy::max)
                        .orElse(null)
        );
    }

    /**
     * 초기 수량 유효성 (신규 스택 생성 시)
     */
    public void assertValidInitialQuantity(int quantity) {
        if (!stackable) {
            if (quantity != 1) {
                throw new DomainException(InventoryError.INVALID_STACK_RULE);
            }
            return;
        }
        if (quantity < 1 || quantity > maxStack) {
            throw new DomainException(InventoryError.INVALID_STACK_RULE);
        }
    }

    /**
     * 현재 스택에서 더 넣을 수 있는 여유량
     */
    public int spaceInStack(int currentQuantity) {
        return stackable ? Math.max(0, maxStack - currentQuantity) : 0;
    }

    /**
     * 현재 스택에 넣을 때 상한을 고려한 실제 추가량
     */
    public int clampAddToLimit(int currentQuantity, int wantToAdd) {
        return stackable ? Math.max(0, Math.min(spaceInStack(currentQuantity), wantToAdd)) : 0;
    }

    /**
     * 내구도 정상화/검증 (정책이 없으면 null 유지)
     */
    public Integer normalizedDurability(Integer given) {
        if (maxDurability == null) {
            return null;
        }

        int v = (given == null) ? maxDurability : given;

        if (v < 0 || v > maxDurability) {
            throw new DomainException(InventoryError.DURABILITY_POLICY);
        }

        return v;
    }

    /**
     * 같은 스택 키(귀속/속성 동일) 여부
     */
    public boolean sameStackKey(
            boolean boundA, Map<String,Object> attrsA,
            boolean boundB, Map<String,Object> attrsB
    ) {
        var a = (attrsA == null) ? Map.of() : attrsA;
        var b = (attrsB == null) ? Map.of() : attrsB;
        return stackable && (boundA == boundB) && Objects.equals(a, b);
    }

    /**
     * (사전 용량 계산용) 기존 스택 채운 뒤 남은 수량이 새 스택을 몇 개 필요로 하는지
     */
    public int estimateNewStacksNeeded(int remainingAfterExisting) {
        if (!stackable) {
            return remainingAfterExisting; // 비스택: 1개당 1슬롯
        }

        return (remainingAfterExisting + maxStack - 1) / maxStack;
    }
}
