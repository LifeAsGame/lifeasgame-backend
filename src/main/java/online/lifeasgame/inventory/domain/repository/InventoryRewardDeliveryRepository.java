package online.lifeasgame.inventory.domain.repository;

import online.lifeasgame.inventory.domain.InventoryRewardDelivery;

import java.util.Optional;

public interface InventoryRewardDeliveryRepository {

    Optional<InventoryRewardDelivery> findByRewardLineId(Long rewardLineId);

    InventoryRewardDelivery save(InventoryRewardDelivery delivery);
}
