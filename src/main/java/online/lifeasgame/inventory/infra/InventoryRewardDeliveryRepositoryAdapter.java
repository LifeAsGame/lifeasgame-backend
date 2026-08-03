package online.lifeasgame.inventory.infra;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.inventory.domain.InventoryRewardDelivery;
import online.lifeasgame.inventory.domain.repository.InventoryRewardDeliveryRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRewardDeliveryRepositoryAdapter
        implements InventoryRewardDeliveryRepository {

    private final JpaInventoryRewardDeliveryRepository jpaRepository;

    @Override
    public Optional<InventoryRewardDelivery> findByRewardLineId(Long rewardLineId) {
        return jpaRepository.findByRewardLineId(rewardLineId);
    }

    @Override
    public InventoryRewardDelivery save(InventoryRewardDelivery delivery) {
        return jpaRepository.saveAndFlush(delivery);
    }
}
