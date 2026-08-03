package online.lifeasgame.inventory.application.internal;

public interface InventoryRewardDeliveryApi {

    RewardDeliveryResult deliverReward(
            Long rewardLineId,
            Long playerId,
            String itemCode,
            long quantity
    );

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
}
