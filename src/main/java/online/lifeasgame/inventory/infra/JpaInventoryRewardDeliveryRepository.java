package online.lifeasgame.inventory.infra;

import online.lifeasgame.inventory.domain.InventoryRewardDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaInventoryRewardDeliveryRepository
        extends JpaRepository<InventoryRewardDelivery, Long> {

    Optional<InventoryRewardDelivery> findByRewardLineId(Long rewardLineId);
}
