package online.lifeasgame.reward.application;

import lombok.RequiredArgsConstructor;
import online.lifeasgame.core.error.DomainException;
import online.lifeasgame.inventory.application.internal.InventoryRewardDeliveryApi;
import online.lifeasgame.reward.application.result.RewardSettlementItemProcessResult;
import online.lifeasgame.reward.domain.RewardSettlement;
import online.lifeasgame.reward.domain.RewardSettlementLine;
import online.lifeasgame.reward.domain.error.RewardError;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RewardSettlementItemProcessAttempt {

    private final RewardSettlementReader settlementReader;
    private final RewardSettlementWriter settlementWriter;
    private final InventoryRewardDeliveryApi inventoryDeliveryApi;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RewardSettlementItemProcessResult process(
            Long settlementId,
            Long lineId
    ) {
        RewardSettlement settlement = settlementReader
                .getByIdForUpdateOrThrow(settlementId);
        RewardSettlementLine line = settlement.getLineByIdOrThrow(lineId);

        if (line.isItemSucceeded()) {
            InventoryRewardDeliveryApi.RewardDeliveryReceipt receipt =
                    inventoryDeliveryApi.findRewardDelivery(lineId)
                            .orElseThrow(this::inconsistent);
            assertMatches(
                    settlement,
                    line,
                    receipt.rewardLineId(),
                    receipt.playerId(),
                    receipt.itemId(),
                    receipt.itemCode(),
                    receipt.quantity()
            );
            return result(
                    settlement,
                    line,
                    receipt.deliveryId(),
                    true
            );
        }

        line.isItemProcessingRequired();
        InventoryRewardDeliveryApi.RewardDeliveryResult delivery =
                inventoryDeliveryApi.deliverReward(
                        lineId,
                        settlement.getPlayerId(),
                        line.getItemCode(),
                        line.getAmount()
                );
        assertMatches(
                settlement,
                line,
                delivery.rewardLineId(),
                delivery.playerId(),
                delivery.itemId(),
                delivery.itemCode(),
                delivery.quantity()
        );

        settlement.markItemLineSucceeded(lineId);
        settlementWriter.saveAndFlush(settlement);
        return result(
                settlement,
                line,
                delivery.deliveryId(),
                delivery.replayed()
        );
    }

    private void assertMatches(
            RewardSettlement settlement,
            RewardSettlementLine line,
            Long rewardLineId,
            Long playerId,
            Long itemId,
            String itemCode,
            long quantity
    ) {
        if (!line.getId().equals(rewardLineId)
                || !settlement.getPlayerId().equals(playerId)
                || !line.getItemId().equals(itemId)
                || itemCode == null
                || !line.getItemCode().equals(itemCode.strip())
                || line.getAmount() != quantity) {
            throw inconsistent();
        }
    }

    private DomainException inconsistent() {
        return new DomainException(
                RewardError.REWARD_SETTLEMENT_ITEM_DELIVERY_INCONSISTENT
        );
    }

    private RewardSettlementItemProcessResult result(
            RewardSettlement settlement,
            RewardSettlementLine line,
            Long deliveryId,
            boolean replayed
    ) {
        return new RewardSettlementItemProcessResult(
                settlement.getId(),
                line.getId(),
                settlement.getPlayerId(),
                line.getStatus(),
                settlement.getStatus(),
                line.getItemId(),
                line.getItemCode(),
                line.getAmount(),
                replayed,
                deliveryId
        );
    }
}
