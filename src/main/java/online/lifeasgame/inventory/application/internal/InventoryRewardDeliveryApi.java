package online.lifeasgame.inventory.application.internal;

import java.util.Optional;

public interface InventoryRewardDeliveryApi {

    RewardDeliveryResult deliverReward(
            Long rewardLineId,
            Long playerId,
            String itemCode,
            long quantity
    );

    Optional<RewardDeliveryReceipt> findRewardDelivery(Long rewardLineId);

    record RewardDeliveryResult(
            Long deliveryId,
            Long rewardLineId,
            Long playerId,
            Long itemId,
            String itemCode,
            long quantity,
            boolean replayed
    ) {
    }

    record RewardDeliveryReceipt(
            Long deliveryId,
            Long rewardLineId,
            Long playerId,
            Long itemId,
            String itemCode,
            long quantity
    ) {
    }
}
